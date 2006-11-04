/*
 * Created on 16.06.2005
 *
 */
package com.jopdesign.sys;

import util.Timer;


/**
 * @author Martin Schoeberl (martin@jopdesign.com)
 *
 */
public class GC {
	
	
	static int mem_start;		// read from memory
static int mem_end;
	// get a effective heap size with fixed handle count
	// for our RT-GC tests
	static int full_heap_size;

	static final int HANDLE_SIZE = 7;

	/**
	 * The handle contains following data:
	 * 0 pointer to the object in the heap or 0 when the handle is free
	 * 1 pointer to the method table or length of an array
	 * 
	 * 2 size - could be in class info
	 *   + GC mark (could be part of the object pointer)
	 * 3 type info: object, primitve array or ref array
	 * 4 pointer to next handle of same type (used or free)
	 * 
	 * follwoing is NOT true
	 * 3 flags to mark reference fields - could be in class info
	 *   -1 means more than 31 fields -> look in class info....
	 */
	static final int OFF_PTR = 0;
	static final int OFF_MTAB_LEN = 1;
	static final int OFF_SIZE = 2;
	static final int OFF_TYPE = 3;
	
	// TODO: use MTAB and size for objects and arrays.
	// don't reuse MTAB as array size as the array size
	// could be 0 and this would be a marker for a free
	// handle. Rethink and write it!!!!!
	// mark() also uses the pointer to the method table at 
	// address-1!
	// size != array length (think about long/double)
	
	// use array types 4..11 are standard boolean to long
	// our addition:
	// 1 reference
	// 0 a plain object
	static final int IS_OBJ = 0;
	static final int IS_REFARR = 1;
	
	//!!! be carefule when changing the handle structure, it's
	// used in System.arraycopy() and probably in jvm.asm!!!
	
	/**
	 * Free and Use list.
	 */
	static final int OFF_NEXT = 4;
	/**
	 * Threading the gray list. End of list is 'special' value -1.
	 * 0 means not in list.
	 */
	static final int OFF_GRAY = 5;
	/**
	 * Special end of list marker -1
	 */
	static final int GRAY_END = -1;
	/**
	 * The mark field (don't use size at the moment)
	 */
	static final int OFF_MARK = 6;

	
	static final int TYPICAL_OBJ_SIZE = 10;
	static int handle_cnt;
	/**
	 * Size of one semi-space, complete heap is two times
	 * the semi_size
	 */
	static int semi_size;
	
	
	static int heapStartA, heapStartB;
	static boolean useA;

	static int fromSpace;
	/**
	 * Points to the start of the to-space after
	 * a flip. Objects are copied from the start
	 * and heapPtr is inecremented
	 */
	static int heapPtr;
	/**
	 * Points to the end of the to-space after
	 * a flip. Objects are allocated from the end
	 * and allocPtr gets decremented.
	 */
	static int allocPtr;
	
	static int freeList;
	static int useList;
	static int grayList;
	
	static int addrStaticRefs;
	
	static Object mutex;
	
	static boolean concurrentGc;
	
	static int startTime;
	
	static int roots[];

	static void init(int mem_size, int addr) {
		
		addrStaticRefs = addr;
mem_end = mem_size-1;
		mem_start = Native.rdMem(0);
		full_heap_size = mem_size-mem_start;
		handle_cnt = full_heap_size/2/(TYPICAL_OBJ_SIZE+HANDLE_SIZE);
		semi_size = (full_heap_size-handle_cnt*HANDLE_SIZE)/2;
		
		heapStartA = mem_start+handle_cnt*HANDLE_SIZE;
		heapStartB = heapStartA+semi_size;
		
		log("");
		log("handle start ", mem_start);
		log("heap start", heapStartA);
		log("heap size (bytes)", semi_size*4*2);
		
// we canot call System.out here!
// System.out.println("Heap: "+heapStartA+" "+heapStartB+" "+(heapStartB+heap_size));
// System.out.println("Size: "+heap_size+" words");
		useA = true;
		heapPtr = heapStartA;
		fromSpace = heapStartB;
		allocPtr = heapPtr+semi_size;
		
		freeList = 0;
		useList = 0;
		grayList = GRAY_END;
		for (int i=0; i<handle_cnt; ++i) {
			int ref = mem_start+i*HANDLE_SIZE;
			addToFreeList(ref);
			Native.wrMem(0, ref+OFF_GRAY);
			Native.wrMem(0, ref+OFF_MARK);
		}
		// clean the heap
		for (int i=heapPtr; i<allocPtr; ++i) {
			Native.wrMem(0, i);
		}
		concurrentGc = false;
		
		// allocate the monitor
		mutex = new Object();
		
		startTime = Native.rd(Const.IO_US_CNT);
	}
	
	public static Object getMutex() {
		return mutex;
	}
	
	static void addToFreeList(int ref) {
		// pointer to former freelist head
		Native.wrMem(freeList, ref+OFF_NEXT);
		// mark handle as free
		Native.wrMem(0, ref+OFF_PTR);
		freeList = ref;
	}
	static void addToUseList(int ref) {		
		// add to used list
		Native.wrMem(useList, ref+OFF_NEXT);
		useList = ref;
	}
	static void addToGrayList(int ref) {
		// only objects not allready in the gray list
		// are added
		if (Native.rdMem(ref+OFF_GRAY)==0) {
			// pointer to former freelist head
			Native.wrMem(grayList, ref+OFF_GRAY);
			grayList = ref;			
//			log("addGray", ref);
		} else {
//			log("already in gray list", ref);
		}
	}
	
	static int getHandle(int ref, int size) {
		
// JVMHelp.wrByte(Native.getSP());
		int addr = freeList;
		freeList = Native.rdMem(freeList+OFF_NEXT);
if (addr<mem_start || addr>=heapStartA) JVMHelp.wr("Problem getHandle");
		// pointer to real object, also marks it as non free
		Native.wrMem(ref, addr); // +OFF_PTR
		// should be from the class info
		Native.wrMem(size, addr+OFF_SIZE);
		// add to used list
		addToUseList(addr);
		return addr;
	}
	
	
	
	/**
	 * Add object to the gray list/stack
	 * @param ref
	 */
	static void push(int ref) {
		
		// if (ref==0) return;		// that's a null pointer
		// Only objects that are referenced by a handle in the
		// handle area are considered for GC.
		// Null pointer and references to static strings are not
		// investigated.
		if (ref<mem_start || ref>=mem_start+handle_cnt*HANDLE_SIZE) return;
		// Is this handle on the free list?
		// Is possible when using conservative stack scanning
		if (Native.rdMem(ref+OFF_PTR)==0) return;
		
		addToGrayList(ref);
		
	}
	/**
	 * Get one (the top) element from the gray list/stack
	 * @return
	 */
	static int pop() {

		int addr = grayList;
		grayList = Native.rdMem(addr+OFF_GRAY);
		Native.wrMem(0, addr+OFF_GRAY);		// mark as not in list
//		log("pop", addr);
		return addr;
	}
	
	static void getRoots() {

		synchronized (mutex) {

			int i, j;
			
			// add static refs to root list
			int addr = Native.rdMem(addrStaticRefs);
			int cnt = Native.rdMem(addrStaticRefs+1);
			for (i=0; i<cnt; ++i) {
				push(Native.rdMem(addr+i));
			}
			// add complete stack of the current thread to the root list
//			roots = GCStkWalk.swk(RtThreadImpl.getActive(),true,false);
			i = Native.getSP();			
			for (j = 128; j <= i; ++j) {
				// disable the if when not using gc stack info
//				if (roots[j - 128] == 1) {
					push(Native.rdIntMem(j));
//				}
			}
			// Stacks from the other threads
			cnt = RtThreadImpl.getCnt();
			
			for (i = 0; i < cnt; ++i) {
				if (i != RtThreadImpl.getActive()) {
					// can we allocate objects here???
					// better don't do it....
					// System.out.print("thread stack ");
					// System.out.println(i);
					int[] mem = RtThreadImpl.getStack(i);
					int sp = RtThreadImpl.getSP(i) - 128; // sp starts at 128

//					roots = GCStkWalk.swk(i, false, false);

					// System.out.print("sp=");
					// System.out.println(sp);
					for (j = 0; j <= sp; ++j) {
						// disable the if when not using gc stack info
//						if (roots[j] == 1) {
							push(mem[j]);
//						}
					}
				}
			}

			// TODO: and what happens when the stack gets changed during
			// GC?
		}
	}
	
	static void mark() {
		
		int i;
		
		getRoots();
		while (grayList!=GRAY_END) {
			int ref = pop();
			int mark = Native.rdMem(ref+OFF_MARK);
			if (mark!=0) {
				// allready marked
				continue;
			}
			Native.wrMem(1, ref+OFF_MARK);
			
			// get pointer to object
			int addr = Native.rdMem(ref);
			int flags = Native.rdMem(ref+OFF_TYPE);
			if (flags==IS_REFARR) {
				// is an array of references
				int size = Native.rdMem(ref+OFF_MTAB_LEN);
				for (i=0; i<size; ++i) {
					push(Native.rdMem(addr+i));
				}
				// However, multinewarray does probably NOT work
			} else if (flags==IS_OBJ){		
				// it's a plain object
				
				// get pointer to method table
				flags = Native.rdMem(ref+1);
				// get real flags
				flags = Native.rdMem(flags-2);
				
				for (i=0; flags!=0; ++i) {
					if ((flags|1)!=0) {
						int child = Native.rdMem(addr+i);
						push(child);
					}
					flags >>>= 1;
				}				
			} else {
				// it's a plain value array
			}
		}
	}
	
	/**
	 * Sweep through the handle list.
	 */
	static void sweep() {

		int use = 0;
		int free = 0;
		
		synchronized (mutex) {
			int ref = useList;
			useList = 0;
			while (ref!=0) {
				int mark = Native.rdMem(ref+OFF_MARK);
				// read next element, as it is destroyed
				// by addTo*List()
				int next = Native.rdMem(ref+OFF_NEXT);
				if (mark!=0) {
					Native.wrMem(0, ref+OFF_MARK);
					addToUseList(ref);
					++use;
				} else {
					addToFreeList(ref);
					++free;
				}
				ref = next;
			}
		}
		System.out.print("used handles=");
		System.out.println(use);
		System.out.print("free handles=");
		System.out.println(free);
	}
	/**
	 * switch from-space and to-space
	 */
	static void flip() {
//		log("flip");
		synchronized (mutex) {
			useA = !useA;
			if (useA) {
				heapPtr = heapStartA;
				fromSpace = heapStartB;
			} else {
				heapPtr = heapStartB;			
				fromSpace = heapStartA;
			}
			allocPtr = heapPtr+semi_size;
		}
	}

	/**
	 * Copy all objects in the useList to the
	 * to-space.
	 */
	static void compact() {

		flip();
		
		int ref = useList;
		while (ref!=0) {
//			log("move", ref);
			int addr = Native.rdMem(ref+OFF_PTR);
//			System.out.println(ref+" move from "+addr+" to "+heapPtr);
			int size = Native.rdMem(ref+OFF_SIZE);
			synchronized (mutex) {
				for (int i=0; i<size; ++i) {
					int val = Native.rdMem(addr+i);
					Native.wrMem(val, heapPtr+i);
if (heapPtr+i<heapStartA || heapPtr+i>=mem_end) JVMHelp.wr("Problem compact");
				}
				// update object pointer to the new location
				Native.wrMem(heapPtr, ref+OFF_PTR);
if (ref<mem_start || ref>=heapStartA) JVMHelp.wr("Problem compact 2");
				heapPtr += size;
			}
			ref = Native.rdMem(ref+OFF_NEXT);
		}
		// clean the from-space to prepare for the next
		// flip
		for (int i=fromSpace; i<fromSpace+semi_size; ++i) {
			Native.wrMem(0, i);
		}
		// for tests clean also the remainig memory in the to-space??
//		for (int i=heapPtr; i<allocPtr; ++i) {
//			Native.wrMem(0, i);
//		}
	}

	public static void setConcurrent() {
		concurrentGc = true;
	}
	static void gc_alloc() {
		log("");
		log("GC allocation triggered");
		if (concurrentGc) {
			log("meaning out of memory for RT-GC");
			dump();
			System.exit(1);	
		} else {
			gc();			
		}
	}

	public static void gc() {
		log("GC called - free memory:", freeMemory());

		mark();
		sweep();
		compact();			

		log("GC end - free memory:",freeMemory());
		
	}
	
	static int free() {
		return allocPtr-heapPtr;
	}
	
	/**
	 * Allocate a new Object. Invoked from JVM.f_new(cons);
	 * @param cons pointer to class struct
	 * @return address of the handle
	 */
	static int newObject(int cons) {
//JVMHelp.wr('.');
		int size = Native.rdMem(cons);			// instance size
		
//System.out.println("new "+heapPtr+" size "+size);
		if (heapPtr+size >= allocPtr) {
			gc_alloc();
		}
		if (heapPtr+size >= allocPtr) {
			// still not enough memory
			System.out.println("Out of memory error!");
			System.exit(1);
		}
		if (freeList==0) {
			System.out.println("Run out of handles!");
			// is this a good place to call gc????
			// better check available handles on newObject
			gc_alloc();
		}
		if (freeList==0) {
			System.out.println("Still out of handles!");
			System.exit(1);
		}

		// we allocate from the upper part
		allocPtr -= size;
		// we need the object size.
		// in the heap or in the handle structure
		// or retrive it from the class info
		int ref;
		// TODO: shouldn't be the whole newObject synchronized?
		//		Than we can remove the synchronized from JVM.java
		synchronized (mutex) {
			ref = getHandle(allocPtr, size);
			// ref. flags used for array marker
			Native.wrMem(IS_OBJ, ref+OFF_TYPE);
			// pointer to method table in the handle
			Native.wrMem(cons+3, ref+OFF_MTAB_LEN);
		}

		return ref;
	}
	
	static int newArray(int size, int type) {
		
		// we are NOT using JVM var h at address 2 for the
		// heap pointer anymore.
		
		int arrayLength = size;
		
		// long or double array
		if((type==11)||(type==7)) size <<= 1;
		// reference array type is 1 (our convention)
		
		if (heapPtr+size >= allocPtr) {
			gc_alloc();
		}
		if (heapPtr+size >= allocPtr) {
			// still not enough memory
			System.out.println("Out of memory error!");
			System.exit(1);
		}
		if (freeList==0) {
			// is this a good place to call gc????
			// better check available handles on newObject
			gc_alloc();
		}
		if (freeList==0) {
			System.out.println("Still out of handles!");
			System.exit(1);
		}

		// we allocate from the upper part
		allocPtr -= size;
		int ref;
		synchronized (mutex) {
			ref = getHandle(allocPtr, size);
			// ref. flags used for array marker
			Native.wrMem(type, ref+OFF_TYPE);
			// array length in the handle
			Native.wrMem(arrayLength, ref+OFF_MTAB_LEN);
		}

		return ref;
		
	}
	

	/**
	 * @return
	 */
	public static int freeMemory() {
		return free()*4;
	}

	/**
	 * @return
	 */
	public static int totalMemory() {
		return semi_size*4;
	}
	
	static int logCnt;
	
	/**
	 * Log all array allocations in the remaining free memory
	 * with the us time stamp and the free memory of the to-space
	 */
	static void logAlloc() {
		
		// free memory after the heap
		int addr = heapStartB+semi_size+logCnt;
		Native.wrMem((Native.rd(Const.IO_US_CNT)-startTime), addr);
		Native.wrMem(freeMemory(), addr+1);
		logCnt += 2;
		
//		JVMHelp.wr("alloc: ");
//		JVMHelp.wrSmall((Native.rd(Const.IO_US_CNT)-startTime)>>10);
//		JVMHelp.wr(";");
//		JVMHelp.wrSmall(freeMemory());
//		JVMHelp.wr("\n");
	}
	
	/**
	 * Dump the logging from logAlloc().
	 */
	public static void dump() {
		System.out.println("Program end");
		System.out.print("Time [ms];Free Memory [Bytes]");
		// a single lf for file dump
		System.out.print("\n");
		for (int i=0; i<logCnt; i+=2) {
			int addr = heapStartB+semi_size+i;
			int time = Native.rdMem(addr);
			int mem = Native.rdMem(addr+1);
			System.out.print(time/1000);
			System.out.print(";");
			System.out.print(mem);
			// a single lf for file dump
			System.out.print("\n");
		}
	}

	static void log(String s, int i) {
		JVMHelp.wr(s);
		JVMHelp.wr(" ");
		JVMHelp.wrSmall(i);
		JVMHelp.wr("\n");
	}
	static void log(String s) {
		JVMHelp.wr(s);
		JVMHelp.wr("\n");
	}
}

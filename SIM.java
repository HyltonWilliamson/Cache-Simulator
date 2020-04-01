/*This program simulates a cache and returns
 * miss ratio, writes to memory and reads to
 * memory.*/

/*
 * Hylton Williamson
 * EEL4768
 * FAll 2019
 * */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;

public class SIM {
	
	public static void main(String[] args) {
		long cache_Size = Long.parseLong(args[0]);
		int assoc = Integer.parseInt(args[1]);
		// replacement policy 0 = LRU, 1 = FIFO
		int replace_policy = Integer.parseInt(args[2]);
		// write-back policy 0 = write-through, 1 = write-back
		int writeB_policy = Integer.parseInt(args[3]);
		String trace_file = args[4];

		int number_of_sets = (int) (cache_Size / (64 * assoc));

		// Set up cache and metadata
		long[][] cache_tags = new long[number_of_sets][assoc];
		int[][] replace_LRU = new int[number_of_sets][assoc];
		int[][] replace_FIFO = new int[number_of_sets][assoc];
		int[][] dirty_track = new int[number_of_sets][assoc];

		// Set up counters
		float hits = 0;
		float misses = 0;
		float accesses = 0;
		float mem_reads = 0;
		float mem_writes = 0;
		int lru_counter = 1;
		int fifo_pointer = 1;

		try {
			// Read in addresses
			File file = new File(trace_file);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String str;
			
			while ((str = br.readLine()) != null) {
				// Parse data
				char op = str.charAt(0);
				BigInteger address = new BigInteger(str.substring(4),16);
				int set_number = ((address.divide(BigInteger.valueOf(64))).mod(BigInteger.valueOf(number_of_sets))).intValue();// (address/64)%number_of_sets
				long tag_address =  (address.divide(BigInteger.valueOf(64))).longValue();//(address/64)
				
				accesses++;
				boolean access_hit = false;
				int index_hit = -1;
				// Searches the cache for match
				for (int i = 0; i < assoc; i++) {
					
					if (tag_address == cache_tags[set_number][i]) {
						hits++;
						access_hit = true;
						index_hit = i;
						break;
					}
				}

				//Hits
				if (access_hit) {
					
					if (op == 'R') {
						replace_LRU[set_number][index_hit] = lru_counter++;
					} else if (op == 'W' && writeB_policy == 1) {// write-back hit
						dirty_track[set_number][index_hit] = 1;
						replace_LRU[set_number][index_hit] = lru_counter++;
					} else if (op == 'W' && writeB_policy == 0) {// write-through hit
						cache_tags[set_number][index_hit] = tag_address;
						replace_LRU[set_number][index_hit] = lru_counter++;
						mem_writes++;
					}
					//Misses
				} else if (!access_hit) {
					misses++;
					int empty_block = 0;
					int lru_block = 0;
					int fifo_block = 0;
					
					if (op == 'R') {// missed Read
						mem_reads++;
						empty_block = searchForBlock(cache_tags, set_number);

						if (writeB_policy == 1) {// If write-back
							
							if (empty_block != -1) {
								cache_tags[set_number][empty_block] = tag_address;
								dirty_track[set_number][empty_block] = 0;
								replace_LRU[set_number][empty_block] = lru_counter++;
								replace_FIFO[set_number][empty_block] = fifo_pointer++;
							} else if (empty_block == -1) {
								
								if (replace_policy == 0) {// If replace policy is LRU
									lru_block = findMinIndex(replace_LRU, set_number);
									
									if (dirty_track[set_number][lru_block] == 1)
										mem_writes++;
									cache_tags[set_number][lru_block] = tag_address;
									dirty_track[set_number][lru_block] = 0;
									replace_LRU[set_number][lru_block] = lru_counter++;
								} else {// If replace policy is FIFO
									fifo_block = findMinIndex(replace_FIFO, set_number);
									mem_writes++;
									cache_tags[set_number][fifo_block] = tag_address;
									dirty_track[set_number][fifo_block] = 0;
									replace_FIFO[set_number][fifo_block] = fifo_pointer++;
								}
							}
						} else if (writeB_policy == 0) {// If write-through
							
							if (empty_block != -1) {
								cache_tags[set_number][empty_block] = tag_address;
							} else if (empty_block == -1) {
								
								if (replace_policy == 0) {// LRU is replace policy
									lru_block = findMinIndex(replace_LRU, set_number);
									cache_tags[set_number][lru_block] = tag_address;
								} else {// FIFO is replace policy
									fifo_block = findMinIndex(replace_FIFO, set_number);
									cache_tags[set_number][fifo_block] = tag_address;
								}
							}
						}

					} else if (op == 'W') {// If writeback and missed Write
						mem_reads++;
						empty_block = searchForBlock(cache_tags, set_number);
						
						if (writeB_policy == 1) {
							
							if (empty_block != -1) {
								cache_tags[set_number][empty_block] = tag_address;
								dirty_track[set_number][empty_block] = 1;
								replace_LRU[set_number][empty_block] = lru_counter++;
								replace_FIFO[set_number][empty_block] = fifo_pointer++;
							} else if (empty_block == -1) {
								
								if (replace_policy == 0) {// If replace policy is LRU
									lru_block = findMinIndex(replace_LRU, set_number);
								
									if (dirty_track[set_number][lru_block] == 1)
										mem_writes++;
									cache_tags[set_number][lru_block] = tag_address;
									dirty_track[set_number][lru_block] = 1;
									replace_LRU[set_number][lru_block] = lru_counter++;
								} else {// If replace policy is FIFO
									fifo_block = findMinIndex(replace_FIFO, set_number);
									mem_writes++;
									cache_tags[set_number][fifo_block] = tag_address;
									dirty_track[set_number][fifo_block] = 0;
									replace_FIFO[set_number][fifo_block] = fifo_pointer++;
								}
							}
						}else if(writeB_policy == 0) {//Write-Through
							mem_writes++;
							
							if (empty_block != -1) {//Empty spot found
								cache_tags[set_number][empty_block] = tag_address;
							} else if (empty_block == -1) {// Implement LRU or FIFO to write over entry
								
								if (replace_policy == 0) {// Replace policy is LRU
									lru_block = findMinIndex(replace_LRU, set_number);
									cache_tags[set_number][lru_block] = tag_address;
								} else {// Replace policy is FIFO
									fifo_block = findMinIndex(replace_FIFO, set_number);
									cache_tags[set_number][fifo_block] = tag_address;
								}
							}
						}

					}
				}
			}

		} catch (Exception e) {
			System.out.println(e);
		}
		System.out.println(misses / (accesses));
		System.out.println(mem_writes);
		System.out.println(mem_reads);
	}

	public static int findMinIndex(int[][] LRU, int set) {
		int currentValue = LRU[set][0];
		int smallestIndx = 0;
		for (int i = 0; i < LRU[set].length; i++) {
			if (LRU[set][i] < currentValue) {
				currentValue = LRU[set][i];
				smallestIndx = i;
			}
		}
		return smallestIndx;
	}

	public static int searchForBlock(long[][] cache, int set) {
		for (int i = 0; i < cache[set].length; i++) {
			if (cache[set][i] == 0) {// There's an empty block
				return i;
			}
		}
		return -1;
	}

	public static BigInteger atoi(String str) {
		return new BigInteger(str);
	}

	public static int log2(int x) {
		return (int) (Math.log(x) / Math.log(2));
	}

}

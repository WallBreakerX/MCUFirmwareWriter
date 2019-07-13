package com.example.myfirmwarewriting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;


public class Hex2Bin {

	final static byte length_index = 0;    /* ��Ҫ����Ϊstatic����, �ſ�����list����Ϊ����ֱ��ʹ�� */
	final static byte address_MSB_index = 1;
	final static byte address_LSB_index = 2;
	final static byte type_index = 3;
	final static byte base_address_MSB_index = 4;
	final static byte base_address_LSB_index = 5;

	final static byte NO_ADDRESS_TYPE_SELECTED = 0;
	final static byte LINEAR_ADDRESS = 1;
	final static byte SEGMENTED_ADDRESS = 2;

	int starting_address = 0;

	public enum RecordType {
		Data_Rrecord,
		EOF_Record,
		Extended_Segment_Address_Record,
		Start_Segment_Address_Record,
		Extended_Linear_Address_Record,
		Start_Linear_Address_Record;    /*java��ö�����Ͷ����﷨��C�е���Щ����, ���һ����Ա������һ��';'��*/
	}

	public byte[] transform(String filepath) {
		// TODO Auto-generated method stub
		int data_bytes, type, checksum;
		int first_word, address;
		//ArrayList<Byte> data = null;
		byte seg_lin_select = NO_ADDRESS_TYPE_SELECTED;

		long lowest_address, highest_address, segment, upper_address;
		long phys_addr;
		int Records_Start;
		int min_block_size;
		int max_length;

		address = 0;
		first_word = 0;
		segment = 0;
		upper_address = 0;
		highest_address = 0;
		lowest_address = Long.MAX_VALUE;

		try {
			FileReader fr = new FileReader(filepath);
			File file = new File("/storage/emulated/0/hextt.bin");
			BufferedReader br = new BufferedReader(fr);
			FileOutputStream fops = new FileOutputStream(file);


			String buffer = null;
			byte hex[] = null;
			ArrayList<Integer> al = new ArrayList<Integer>();
			ArrayList<Record> result = new ArrayList<Record>();
			Record record = new Record(0, null);
			while ((buffer = br.readLine()) != null) {
				hex = buffer.substring(1).getBytes();
				for (int i = 0; i < hex.length; i++) {
					if (hex[i] >= '0' && hex[i] <= '9') {
						hex[i] = (byte) (hex[i] - '0');
					} else if (hex[i] >= 'A' && hex[i] <= 'Z') {
						hex[i] = (byte) (hex[i] - '0' - 7);
					}
					System.out.printf(String.valueOf(hex[i]));
				}

				for (int i = 0; i < hex.length - 1; i += 2) {
					al.add((Integer) ((hex[i] << 4) | hex[i + 1]));
				}
				data_bytes = al.get(length_index);
				type = al.get(type_index);
				checksum = al.get(al.size() - 1);    //���һλ��У���
				first_word = ((al.get(address_MSB_index) << 8) | al.get(address_LSB_index)) & 0xFFFF;    //��1��2λ�ǵ�ַ

				int sum = 0;
				for (int i = 0; i < (al.size() - 1); i++) {
					sum += al.get(i);
				}

				if (((sum + checksum) & 0xFF) != 0)
					System.out.println("The checksum of hex file exist error!");

				switch (RecordType.values()[type]) {    //values()�� ��̬����������һ������ȫ��ö��ֵ������
					case Data_Rrecord:
						if (data_bytes == 0)
							break;

						address = first_word;
						if (seg_lin_select == SEGMENTED_ADDRESS) {
							phys_addr = (segment << 4) + address;
						} else {
							phys_addr = (upper_address << 16) + address;    //32-bit address
						}

						if ((phys_addr) < lowest_address)    //�ҵ���͵ĵ�ֵַ
							lowest_address = phys_addr;

						long temp = phys_addr + data_bytes - 1;  //�ҵ���ߵĵ�ֵַ
						if (temp > highest_address)
							highest_address = temp;
						if (data_bytes > 0) {
							for (int j = 0; j < 4; j++)
								al.remove(0);    /* �Ƴ�ĳ��Ԫ�غ�, �����Ԫ�ؾͻ��Զ���ǰ�ƶ� */

							al.remove(al.size() - 1);    //remove the byte of checksum

							record.setAddressAndList(phys_addr, (ArrayList<Integer>) al.clone());
							result.add((Record) record.clone());
						}
						break;

					case EOF_Record:

						break;

					case Extended_Segment_Address_Record:
						if (seg_lin_select == NO_ADDRESS_TYPE_SELECTED) {
							seg_lin_select = SEGMENTED_ADDRESS;
						}

						if (seg_lin_select == SEGMENTED_ADDRESS) {
							segment = (al.get(base_address_MSB_index) << 4) | al.get(base_address_LSB_index);
							phys_addr = (segment << 4);
						} else {
							System.out.printf("Ignored extended linear address record %d\n", data_bytes);
						}

						break;

					case Start_Segment_Address_Record:

						break;

					case Extended_Linear_Address_Record:
						if (seg_lin_select == NO_ADDRESS_TYPE_SELECTED)
							seg_lin_select = LINEAR_ADDRESS;

						if (seg_lin_select == LINEAR_ADDRESS) {
							upper_address = (al.get(base_address_MSB_index) << 4) | al.get(base_address_LSB_index);
							phys_addr = (upper_address << 16);
						}
						break;

					case Start_Linear_Address_Record:

						break;

					default:
						break;
				}
				al.clear();
			}

			max_length = (int) (highest_address - lowest_address + 1);
			ArrayList<Integer> list_temp = new ArrayList<>();
			list_temp.ensureCapacity(max_length);
			for (int i = 0; i < max_length; i++) {
				list_temp.add(0xFF);
			}

			for (Record r : result) {
				long addr = r.getAddress();
				addr -= lowest_address;
				ArrayList<Integer> al_temp = r.getList();
				for (int i = 0; i < al_temp.size(); i++) {
					list_temp.set((int) (addr + i), al_temp.get(i));
				}
			}

			byte t1 = (byte) 0xFF;
			byte bindate[] = new byte[list_temp.size()];
			int count = 0;
			for (Integer b : list_temp) {
				t1 = (byte) (b & 0xFF);
				fops.write(b);
				bindate[count] = t1;
				count += 1;
			}
			br.close();
			fr.close();
			fops.close();
			return bindate;
		} catch (Exception ioe) {
			// TODO: handle exception
			ioe.printStackTrace();
		}
		return null;
	}

	private int AllocateMemoryAndRewind(int low_address, int high_address){
		int length = high_address-low_address+1;
		starting_address = low_address;
		return length;
	}
}

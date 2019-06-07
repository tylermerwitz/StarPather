package main;

import java.util.ArrayList;

public class ComboLib {
	
	private int totalSP = 0;
	private int ones = 0;
	
	public ComboLib (int sp, int o) {
		this.totalSP = sp;
		this.ones = o;
	}
	
	public void returnCombos (ArrayList<String> list) {
		switch (totalSP) {
		case 1:
			list.add("1");
			break;
		case 2:
			list.add("2");
			if (ones > 0) {
				list.add("11");
			}
		case 3:
			list.add("3");
			list.add("21");
			if (ones > 0) {
				list.add("12");
			}
			if (ones > 1) {
				list.add("111");
			}
		}
	}
	
	public void test () {
		for (int i = 1; i < 51; i++) {
			
		}
	}

}

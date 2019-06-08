package ComboLib;

import java.util.ArrayList;

public class ComboLibSearch {

		private int totalSP = 0;
		private int ones = 0;
		
		public ComboLibSearch (int sp, int o) {
			this.totalSP = sp;
			this.ones = o;
		}
		
		public void Search (ArrayList<String> list) {
			if (totalSP < 9) {
				ComboLib1to14 c = new ComboLib1to14 (totalSP, ones);
				c.returnCombos1(list);
			}
			
			else if (totalSP == 9) {
				ComboLib1to14 c = new ComboLib1to14 (totalSP, ones);
				c.returnCombos9(list);
			}
			
			else if (totalSP == 10) {
				ComboLib1to14 c = new ComboLib1to14 (totalSP, ones);
				c.returnCombos10(list);
			}
			
			else if (totalSP == 11) {
				ComboLib1to14 c = new ComboLib1to14 (totalSP, ones);
				c.returnCombos11(list);
			}
			
			else if (totalSP == 12) {
				ComboLib1to14 c = new ComboLib1to14 (totalSP, ones);
				c.returnCombos12(list);
			}
			
			else if (totalSP == 13) {
				ComboLib1to14 c = new ComboLib1to14 (totalSP, ones);
				c.returnCombos13(list);
			}
			
			else if (totalSP == 14) {
				ComboLib1to14 c = new ComboLib1to14 (totalSP, ones);
				c.returnCombos14p1(list);
				if (ones > 3) {
					c.returnCombos14p2(list);
				}
			}
			
			else if (totalSP == 15) {
				ComboLib15 c = new ComboLib15 (totalSP, ones);
				c.returnCombos15p1(list);
				if (ones > 3) {
					c.returnCombos15p2(list);
				}
				if (ones > 5) {
					c.returnCombos15p3(list);
				}
			}
			
			else if (totalSP == 16) {
				ComboLib16p1 c = new ComboLib16p1 (totalSP, ones);
				c.returnCombos16p1(list);
				if (ones > 1) {
					c.returnCombos16p2(list);
				}
				if (ones > 2) {
					c.returnCombos16p3(list);
				}
				if (ones > 3) {
					ComboLib16p2 c2 = new ComboLib16p2 (totalSP, ones);
					c2.returnCombos16p1(list);
				}
				if (ones > 4) {
					ComboLib16p2 c2 = new ComboLib16p2 (totalSP, ones);
					c2.returnCombos16p2(list);
				}
				if (ones > 6) {
					ComboLib16p2 c2 = new ComboLib16p2 (totalSP, ones);
					c2.returnCombos16p2(list);
				}
			}
			
		}
		
}

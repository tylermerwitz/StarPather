package main;

import java.util.ArrayList;

public class PermuTest {
	private int n = 5;   // number of elements to permute.  Let N > 2
	
	public PermuTest () {};

	public void QuickPerm(int[] combo, ArrayList<String> all)
	{
		n = combo.length;
	   int a[] = new int[n];
	   int p[] = new int[n+1];
	   int i, j, tmp; // Upper Index i; Lower Index j

	   for(i = 0; i < n; i++)   // initialize arrays; a[N] can be any type
	   {
	      a[i] = combo[i];   // a[i] value is not revealed and can be arbitrary
	      if (i == 0)
	    	  p[i] = 0;
	      else
	    	  p[i] = combo[i-1];
	   }
	   p[n] = n; // p[N] > 0 controls iteration and the index boundary for i
	   quickCheck(a, 0, 0, all);   // remove comment to display array a[]
	   i = 1;   // setup first swap points to be 1 and 0 respectively (i & j)
	   while(i < n)
	   {
	      p[i]--;             // decrease index "weight" for i by one
	      j = i % 2 * p[i];   // IF i is odd then j = p[i] otherwise j = 0
	      tmp = a[j];         // swap(a[j], a[i])
	      a[j] = a[i];
	      a[i] = tmp;
	      quickCheck(a, j, i,all); // remove comment to display target array a[]
	      i = 1;              // reset index i to 1 (assumed)
	      while (p[i] == 0)       // while (p[i] == 0)
	      {
	         p[i] = i;        // reset p[i] zero value
	         i++;             // set new index value for i (increase by one)
	      } // while(!p[i])
	   } // while(i < N)
	} // QuickPerm()
	
	void quickCheck(int a[], int j, int i, ArrayList<String> all)            
	{
		String s = "";
	   for(int x = 0; x < n; x++) {
	      //System.out.println(a[x]);
	      s = s + a[x];
	   }
	   //System.out.println("   swapped(" + j +", " + i + ")\n");
	   if (!all.contains(s)) {
		   all.add(s);
	   }
	} // display()
	
	public static void main(String[] args) {
		PermuTest pt = new PermuTest();
		ArrayList<String> list = new ArrayList<String>();
		int c[] = {2,2,2,3};
		pt.QuickPerm(c, list);
		for (int i = 0; i < list.size(); i++) {
			System.out.println(list.get(i));
		}
	}

}
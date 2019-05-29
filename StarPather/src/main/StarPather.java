package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class StarPather {

	private int linesRead           = 0;
	private String lastLine         = "";
	private String lastError        = "";

	private int notes = 0;
	private int mult = 1;
	private int numberOfOnes = 0;
	private boolean maxMult = false;
	private int bestScore;

	protected String name = "";
	private StringBuffer output = new StringBuffer();

	private int resolution = 192;
	private double bpm = 0.0;
	private double bps = 0.0;

	private TimeSig ts = new TimeSig();

	private ArrayList<SyncEvent> syncEvents = new ArrayList<SyncEvent>();

	private boolean lastSyncEvent = false;
	private int lastSyncIndex = 0;
	//private SyncEvent current = new SyncEvent();
	//private SyncEvent next = new SyncEvent();

	SortedMap<Integer,SyncEvent> syncMap = new TreeMap<Integer,SyncEvent>();
	SortedMap<Integer,Note> noteMap = new TreeMap<Integer,Note>();
	SortedMap<Integer,StarSection> starMap = new TreeMap<Integer,StarSection>();

	private boolean noWhammy = false;

	public StarPather () {}

	public void appendName (String s) {
		this.name = this.name + s;
	}

	public void copyList (ArrayList<SyncEvent> o, ArrayList<SyncEvent> c) {
		c.clear();

		for (int i = 0; i < o.size(); i++) {
			SyncEvent e = new SyncEvent(o.get(i).getInd(),o.get(i).getType(), o.get(i).getValue());
			c.add(e);
			System.out.println(c.get(i).getValue());
		}
	}
	
	public String getOutput () {
		return this.output.toString();
	}

	public int updateSync (int time, int index) {
		if (!syncEvents.isEmpty()) {

			SyncEvent e = syncEvents.get(index);

			if (index == syncEvents.size() - 1) {
				lastSyncEvent = true;
			}

			if (e.getInd() <= time) {
				if (e.getType().equals("TS")) {
					ts.setTop(e.getValue());
					if (e.getValue2() == 0)
						ts.setBottom(4);
					else if (e.getValue2() == 3)
						ts.setBottom(8);
					else if (e.getValue2() == 4)
						ts.setBottom(16);
					index++;
					//System.out.println("Time: " + ts.top + "/" + ts.bottom);
				}
				else if (e.getType().equals("B")){
					bpm = e.getValue()/1000;
					bps = bpm/60;
					index++;
					//System.out.println("BPM: " + bpm);
				}
			}
		}

		return index;
	}

	public int getTotalSum () {
		int sum = 0;

		for (Map.Entry<Integer, Note> entry : noteMap.entrySet())  {
			Note n = entry.getValue();
			sum = sum + n.getValue();
		}

		return sum;
	}

	public int getValueSum (int min, int max) {
		int sum = 0;

		SortedMap<Integer,Note> subMap = noteMap.subMap(min, max);

		for (Map.Entry<Integer, Note> entry : subMap.entrySet())  {
			Note n = entry.getValue();
			sum = sum + n.getValue();
		}
		
		if (subMap.isEmpty()) {
			return 0;
		}
		Note nn = subMap.get(subMap.lastKey());
		int noteEnd = nn.getTime() + nn.getLength();
		if (max < noteEnd) {
			int lengthDif = noteEnd - max;
			double lv = 1.0 * lengthDif / resolution;
			lv = lv * 25;
			sum = (int) (sum - lv);
		}

		return sum;
	}

	public int getLengthSum (int min, int max) {
		int sum = 0;

		SortedMap<Integer,Note> subMap = noteMap.subMap(min, max);

		for (Map.Entry<Integer, Note> entry : subMap.entrySet())  {
			Note n = entry.getValue();
			sum = sum + n.getLength();
		}

		return sum;
	}

	public void parseFile(InputStream chart) {
		BufferedReader in = null;
		String theline    = null;
		output = new StringBuffer();

		boolean songSection = false;
		boolean syncSection = false;
		boolean eventSection = false;
		boolean notesSection = false;

		StringBuffer songInfo = new StringBuffer();

		try{
			in = new BufferedReader(new InputStreamReader(chart));

			while ((theline = in.readLine()) != null){
				if(theline == null || theline.trim().length()==0) {
					continue;
				}
				this.linesRead++;
				this.lastLine = theline;

				if (theline.startsWith("[Song]")) {
					songSection = true;
					continue;
				}

				if (songSection) {
					if (theline.startsWith("{")) {
						continue;
					}
					else if (theline.startsWith("Name = ")) {
						String songName = theline.substring(9).trim().replace("\"", "");
						appendName(songName);
						songInfo.append(songName);
						continue;
					}
					else if (theline.startsWith("Artist =")) {
						String artistName = " by " + theline.substring(11).trim().replace("\"", "");
						appendName(artistName);
						songInfo.append(artistName);
						continue;
					}
					else if (theline.startsWith("Resolution =")) {
						resolution = Integer.parseInt(theline.substring(15).trim());
						//System.out.println(resolution);
						continue;
					}
					else if (theline.startsWith("}")) {
						songSection = false;
						continue;
					}
					else {
						theline = theline.trim();
						songInfo.append(theline);
						continue;
					}
				}

				if (theline.startsWith("[SyncTrack]")) {
					syncSection = true;
					continue;
				}

				if (syncSection) {
					if (theline.startsWith("{")) {
						continue;
					}

					else if (theline.startsWith("}")) {
						syncSection = false;
						continue;
					}

					else {
						int split = theline.indexOf("=");
						String ind = theline.substring(2,split).trim();
						String type = "";
						String val = "";
						if (theline.charAt(split+2) == 'T') {
							type = "TS";
							val = theline.substring(split+5);
						}
						else if (theline.charAt(split+2) == 'B') {
							type = "B";
							val = theline.substring(split+4);
						}
						else {
							//Throw error?
							continue;
						}
						SyncEvent e = new SyncEvent(ind,type,val);
						syncEvents.add(e);
						continue;
					}

				}

				if (theline.startsWith("[Events]")) {
					eventSection = true;
					continue;
				}

				if (eventSection) {

					if (theline.startsWith("}")) {
						eventSection = false;
						continue;
					}

					else {
						continue;
					}
				}

				if (theline.startsWith("[ExpertSingle]")) {
					notesSection = true;
					continue;
				}

				if (notesSection) {

					if (theline.startsWith("{")) {
						continue;
					}

					if (theline.startsWith("}")) {
						notesSection = false;
						theline = null;
					}

					else {

						int split = theline.indexOf("=");

						int time = Integer.parseInt(theline.substring(2,split).trim());

						if (!lastSyncEvent) {
							lastSyncIndex = updateSync(time,lastSyncIndex);
						}

						if (theline.charAt(split+2) == 'N') {

							String fret = theline.substring(split+4,split+6).trim();

							if (fret.equals("6")) {
								continue;
							}
							else {
								String length = theline.substring(split+6).trim();
								int value = 50;

								if (!length.equals(0)) {
									double lv = Double.parseDouble(length) / resolution;
									lv = lv * 25;
									value = (int) (value + Math.round(lv));
								}

								if (notes == 0) {
									Note n = new Note(time,fret,length,value);
									noteMap.put(time, n);
									notes++;
									continue;
								}

								if (noteMap.containsKey(time)) {
									value = value * mult;
									Note n = new Note(time,fret,length,value);
									n.chord(noteMap.get(time));
									noteMap.replace(time, n);
									continue;
								}
								else {
									if (!maxMult) {
										if (notes >= 30) {
											mult = 4;
											maxMult = true;
										}
										else if (notes >= 20 && mult < 3) {
											mult = 3;
										}
										else if (notes >= 10 && mult < 2) {
											mult = 2;
										}
									}

									value = value * mult;
									Note n = new Note(time,fret,length,value);
									noteMap.put(time, n);
									notes++;
									continue;
								}
							}
						}

						else if (theline.charAt(split+2) == 'S') {
							int l = Integer.parseInt(theline.substring(split+6).trim());

							StarSection ss = new StarSection(time,l);
							starMap.put(time, ss);
						}

					}

				}

			}
		}
		catch (Exception e) {
			StringBuilder errorMsg = new StringBuilder();

			errorMsg.append(e.getMessage() + "\r\n");
			errorMsg.append("Last line processed (" + linesRead + "):\r\n" + this.lastLine + "\r\n");

			for (StackTraceElement ste : e.getStackTrace()){
				errorMsg.append(ste.toString() + "\r\n");
			}

			lastError = errorMsg.toString();
			System.out.println(lastError);
		}
		finally {
			int score = getTotalSum();
			output.append("Notes: " + notes + "\n");
			output.append("Base Score: " + score + "\n\n");
		}
	}

	public int ultraEasyPath () {
		int pathScore = getTotalSum();
		double sp = 0.0;

		lastSyncIndex = 0;

		for (Map.Entry<Integer, StarSection> entry : starMap.entrySet())  {
			StarSection ss = entry.getValue();

			if (ss.getMeasures() == -100) {
				ss.setMeasures(0);
			}

			if (!lastSyncEvent) {
				lastSyncIndex = updateSync(ss.getTime(),lastSyncIndex);
			}

			if (ss.getMeasures() > 0) {
				double lv = ss.addLength();
			}
			sp = sp + ss.getMeasures();
			//output.append(ss.getMeasures());
			if (sp > 8) {
				sp = 8;
			}

			/*if (sp - 2 >= 4) {

			}*/
			if (sp >= 4){
				int active = getActivationNote(ss.getTime(), ss.getLength());
				SortedMap<Integer,Note> subMap = noteMap.subMap(active,noteMap.lastKey());
				active = subMap.firstKey();

				if (!lastSyncEvent) {
					lastSyncIndex = updateSync(active,lastSyncIndex);
				}

				double tsMes = ts.getTop() / ts.getBottom() * 4;
				int splength = (int) Math.round(tsMes * sp * resolution);
				int end = active + splength;

				if (!lastSyncEvent) {
					end = checkSync(active,end,sp);
				}

				checkExtraSP(active,end,sp);

				int spSum = getValueSum(active, end);
				output.append(spSum + "\n");
				pathScore = pathScore + spSum;
				sp = 0;
			}
		}
		output.append("Easy Score: " + pathScore);
		return pathScore;
	}

	public int ultraEasyNoWhammy () {

		int pathScore = getTotalSum();
		double sp = 0.0;

		noWhammy = true;

		lastSyncIndex = 0;

		for (Map.Entry<Integer, StarSection> entry : starMap.entrySet())  {
			StarSection ss = entry.getValue();

			if (ss.getMeasures() == -100) {
				ss.setMeasures(0);
			}

			if (!lastSyncEvent) {
				lastSyncIndex = updateSync(ss.getTime(),lastSyncIndex);
			}

			if (ss.getMeasures() > 2) {
				ss.setMeasures(2);
			}
			sp = sp + ss.getMeasures();
			//System.out.println(sp);
			if (sp > 8) {
				sp = 8;
			}

			/*if (sp - 2 >= 4) {

			}*/
			if (sp >= 4){
				int active = getActivationNote(ss.getTime(), ss.getLength());
				SortedMap<Integer,Note> subMap = noteMap.subMap(active,noteMap.lastKey());
				active = subMap.firstKey();

				if (!lastSyncEvent) {
					lastSyncIndex = updateSync(active,lastSyncIndex);
				}

				double tsMes = ts.getTop() / ts.getBottom() * 4;
				int splength = (int) Math.round(tsMes * sp * resolution);
				int end = active + splength;

				if (!lastSyncEvent) {
					end = checkSync(active,end,sp);
				}

				checkExtraSP(active,end,sp);

				int spSum = getValueSum(active, end);
				pathScore = pathScore + spSum;
				sp = 0;
			}
		}

		output.append("Ultra Easy Score: " + pathScore);
		return pathScore;

	}

	public int ultraEasyFullPath () {
		int pathScore = getTotalSum();
		double sp = 0.0;

		lastSyncIndex = 0;

		for (Map.Entry<Integer, StarSection> entry : starMap.entrySet())  {
			StarSection ss = entry.getValue();

			if (ss.getMeasures() == -100) {
				ss.setMeasures(0);
			}

			if (!lastSyncEvent) {
				lastSyncIndex = updateSync(ss.getTime(),lastSyncIndex);
			}

			if (ss.getMeasures() > 0) {
				double lv = ss.addLength();
			}
			sp = sp + ss.getMeasures();
			//System.out.println(ss.getMeasures());
			if (sp > 8) {
				sp = 8;
			}

			/*if (sp - 2 >= 4) {

			}*/
			if (sp >= 8){
				int active = getActivationNote(ss.getTime() , ss.getLength());
				SortedMap<Integer,Note> subMap = noteMap.subMap(active,noteMap.lastKey());
				active = subMap.firstKey();

				if (!lastSyncEvent) {
					lastSyncIndex = updateSync(active,lastSyncIndex);
				}

				double tsMes = ts.getTop() / ts.getBottom() * 4;
				int splength = (int) Math.round(tsMes * sp * resolution);
				int end = active + splength;

				if (!lastSyncEvent) {
					end = checkSync(active,end,sp);
				}

				checkExtraSP(active,end,sp);

				int spSum = getValueSum(active, end);
				pathScore = pathScore + spSum;
				sp = 0;
			}
		}
		output.append("Easy Full Score: " + pathScore);
		return pathScore;
	}

	public int noSqueezePath () {

		try {
			int pathScore = getTotalSum();

			ArrayList<String> combos = new ArrayList<String>();
			checkStarCombos(combos);
			int bestComboScore = 0;
			String bestPath = "";
			String bestPathLiteral = "";
			String bestPathDetail = "";

			ArrayList<StarSection> ssections = new ArrayList<StarSection>();
			for (Map.Entry<Integer, StarSection> entry : starMap.entrySet()) {
				StarSection ss = entry.getValue();
				ssections.add(ss);
			}

			for (int i = 0; i < combos.size(); i++) {
				String s = combos.get(i);
				int currentSp = 0;
				int totalSp = 0;
				int comboScore = 0;
				String actualPath = "";
				StringBuffer pathDetail = new StringBuffer();
				String pathHeader = "Path " + (i+1) + ": " + s + "\n";

				for (int j = 0; j < s.length(); j++) {
					int x = Character.getNumericValue(s.charAt(j));
					currentSp = x;
					totalSp = totalSp + x;
					double sp = 0.0;
					int active = 0 ;
					bestScore = 0;
					ArrayList<Integer> activations = new ArrayList<Integer>();
					boolean secondLast = false;

					for (int k = totalSp - currentSp; k < totalSp; k++ ) {
						StarSection ss = ssections.get(k);
						if (!lastSyncEvent) {
							lastSyncIndex = updateSync(ss.getTime(),lastSyncIndex);
						}

						double lv = ss.returnLength();
						sp = sp + ss.getMeasures() + lv;

						//Currently using last (1) even if SP is under 4

						if (sp < 4 && currentSp == 1 && totalSp == ssections.size()) {
							j = s.length();
							k = totalSp;
							sp = 0;
						}

						if (sp > 8) {
							sp = 8;
						}

						activations.add(getActivationNote(ss.getTime(),ss.getLength()));

						if (sp >= 4 && k + 2 == totalSp){
							StarSection lastSs = ssections.get(k+1);
							int firstAct = getActivationNote(ss.getTime(),ss.getLength());
							int secondAct = getActivationNote(lastSs.getTime(),lastSs.getLength());

							if (!lastSyncEvent) {
								lastSyncIndex = updateSync(firstAct,lastSyncIndex);
							}

							double currentTsMes = ts.getTop() / ts.getBottom() * 4;
							int currentSplength = (int) Math.round(currentTsMes * sp * resolution);

							SortedMap<Integer,Note> subMap = noteMap.subMap(firstAct,secondAct);
							active = subMap.lastKey() - currentSplength;
							secondLast = true;
						}
						else if (!secondLast){
							active = getActivationNote(ss.getTime(),ss.getLength());
							SortedMap<Integer,Note> subMap = noteMap.subMap(active,noteMap.lastKey());
							active = subMap.firstKey();
						}
					}

					if (!lastSyncEvent) {
						lastSyncIndex = updateSync(active,lastSyncIndex);
					}

					double tsMes = 1.0 * ts.getTop() / ts.getBottom() * 4;
					int splength = (int) Math.round(tsMes * sp * resolution);
					int firstActive = active;
					int lastActive = 0;

					if (totalSp < ssections.size()) {
						StarSection nextSs = ssections.get(totalSp);
						int nextSsActive = getActivationNote(nextSs.getTime(), nextSs.getLength());
						SortedMap<Integer,Note> subMap2 = noteMap.subMap(firstActive,nextSsActive-1);
						lastActive = subMap2.lastKey() - splength;
						SortedMap<Integer,Note> subMap3 = noteMap.subMap(firstActive,lastActive);
						lastActive = subMap3.lastKey();
					}
					else if (sp > 0){
						int last = noteMap.lastKey();
						Note lastNote = noteMap.get(last);
						lastActive = lastNote.getTime() - splength;
						SortedMap<Integer,Note> subMap4 = noteMap.subMap(lastActive,noteMap.lastKey());
						if (!subMap4.isEmpty())
							lastActive = subMap4.firstKey();
					}

					int bestActivation = getHighestScore(firstActive,lastActive,splength);

					if (activations.size()==0) {
						continue;
					}
					int lastStar = activations.get(activations.size()-1);
					String activeNumber = "";
					if (bestActivation < lastStar) {
						for (int m = 0; m < activations.size(); m++) {
							if (bestActivation < activations.get(m)) {
								activeNumber = m + "(" + (currentSp - m) + ")";
							}
						}
					}
					else {
						activeNumber = "" + currentSp;
					}
					if (j!=0) {
						actualPath = actualPath + ", ";
					}
					actualPath = actualPath + activeNumber;
					comboScore = comboScore + bestScore;
					pathDetail.append(activeNumber+"\n");
					//pathDetail.append(bestActivation+"\n");

					Note activeNote = noteMap.get(bestActivation);
					int afterStar = Integer.parseInt(Character.toString(activeNumber.charAt(0)));

					boolean onNote = true;
					int activeBefore = 0;
					if (activeNote == null) {
						SortedMap<Integer,Note> subMap5 = noteMap.subMap(bestActivation,noteMap.lastKey());
						activeNote = subMap5.get(subMap5.firstKey());
						onNote = false;
						activeBefore = activeNote.getTime() - bestActivation;
					}

					if (afterStar != 0) {
						int mapStart = activations.get(afterStar - 1);
						SortedMap<Integer,Note> subMap6 = noteMap.subMap(mapStart+1,activeNote.getTime()+1);
						String noteFret = activeNote.getFret();
						int fretCounter = 0;

						for (Map.Entry<Integer, Note> entry : subMap6.entrySet()) {
							Note nn = entry.getValue();
							if (nn.getFret().equals(noteFret)) {
								fretCounter++;
							}
						}
						if (fretCounter == 0) {
							fretCounter = 1;
						}
						
						String colorFret = "";
						for (int p = 0; p < noteFret.length(); p++ ) {
							Character color = noteFret.charAt(p);
							switch (color) {
							case '0':
								color = 'G';
								break;
							case '1':
								color = 'R';
								break;
							case '2':
								color = 'Y';
								break;
							case '3':
								color = 'B';
								break;
							case '4':
								color = 'O';
								break;
							default:
								color = 'P';
							}
							if (color == 'P' && noteFret.length() == 1) {
								colorFret = "Open";
							}
							else if (color != 'P') {
								colorFret = colorFret + color;
							}
						}
						
						noteFret = colorFret;
							

						String fc = fretCounter + "";
						Character lastNo = fc.charAt(fc.length()-1);
						Character secondLastNo = ' ';
						if (fc.length() > 1) {
							secondLastNo = fc.charAt(fc.length()-2);
						}
						if (lastNo == '1' && secondLastNo != '1') {
							fc = fc + "st";
						}
						else if (lastNo == '2' && secondLastNo != '1') {
							fc = fc + "nd";
						}
						else if (lastNo == '3' && secondLastNo != '1') {
							fc = fc + "rd";
						}
						else {
							fc = fc + "th";
						}

						String activeDetail = "";
						double beatsBefore = 1.0 * activeBefore/resolution;
						
						if (!onNote && beatsBefore >= 0.5) {
							activeDetail = beatsBefore + " Beats Before ";
						}
						activeDetail = activeDetail + fc + " " + noteFret;

						pathDetail.append(activeDetail+"\n\n");
					}
				}
				//System.out.println(pathHeader);
				//System.out.println(pathDetail.toString());
				if (comboScore > bestComboScore) {
					bestComboScore = comboScore;
					bestPath = actualPath;
					bestPathLiteral = s;
					bestPathDetail = pathDetail.toString();
				}
			}
			pathScore = pathScore + bestComboScore;
			output.append("Best Path: " + bestPath + "\n");
			output.append("Score = " + pathScore + "\n\n");
			output.append(bestPathDetail);
			return pathScore;
		} catch (Exception e) {
			
			e.printStackTrace();
			return 0;
		}

	}

	public int getHighestScore (int firstAct, int lastAct, int sp) {
		int activationPoint = 0;
		int highestScore = 0;

		for (int i = firstAct; i <= lastAct; i++) {
			int sectionScore = getValueSum(i,i+sp);
			if (sectionScore > highestScore) {
				highestScore = sectionScore;
				activationPoint = i;
			}
			else if (sectionScore == highestScore) {
				Note note = noteMap.get(i);
				Note note2 = noteMap.get(activationPoint);
				if (note2 == null && note != null) {
					activationPoint = i;
				}
			}
		}

		bestScore = highestScore;
		return activationPoint;
	}

	public double spLeft (int active, int current, double sp) {
		double newsp = 0.0;

		int spsplit = current - active;

		if (spsplit < 0) {
			return 0;
		}

		else {
			double tsMes = ts.getTop() / ts.getBottom() * 4;
			double meselapsed = spsplit / resolution / tsMes;
			newsp = sp - meselapsed;

			return newsp;
		}
	}
	
	public int getActivationNote (int time, int length) {
		int a = 0;
		
		SortedMap<Integer,Note> subMap = noteMap.subMap(time,time+length);
		a  = subMap.lastKey();
		
		return a;
	}

	public void printStarMap () {
		int i = 1;
		for (Map.Entry<Integer, StarSection> entry : starMap.entrySet())  {
			StarSection ss = entry.getValue();
			System.out.println(i);
			i++;
			System.out.println(ss.getTime());
			double measures = ss.getMeasures() + ss.returnLength();
			System.out.println(measures);
			System.out.println("\n");
		}
	}

	public void checkStarCombos (ArrayList<String> CList) {

		ArrayList<Integer> ones = CheckOnes();
		numberOfOnes = ones.size();
		int n = starMap.size();
		int arr[] = new int [n];
		ArrayList<int[]> comboList = new ArrayList<int[]>();

		findCombinationsUtil(arr,comboList,0,n,n);

		ArrayList<String> allCombos = new ArrayList<String>();

		for (int i = 0; i < comboList.size(); i++) {
			int[] str = new int[comboList.get(i).length];
			boolean sameDigits = true;
			if (starMap.size() > 9) {
				boolean skip = false;
				for (int q = 0; q < comboList.get(i).length; q++) {
					int checkOverTen = comboList.get(i)[q];
					if (checkOverTen >= 10) {
						skip = true;
					} 
				}
				if (skip) {
					continue;
				}
			}
			for (int j = 0; j < str.length; j++) {
				str[j] = comboList.get(i)[j];
				if (j > 0) {
					if (str[j] != str[j-1]) {
						sameDigits = false;
					}
				}
			}
			if (str.length == 1) {
				String s = Integer.toString(str[0]);
				if (!allCombos.contains(s)) {
					allCombos.add(s);
				}
			}
			else if (str.length == 2) {
				String s = Integer.toString(str[0]) + Integer.toString(str[1]);
				if (!allCombos.contains(s)) {
					allCombos.add(s);
				}
				String s2 = Integer.toString(str[1]) + Integer.toString(str[0]);
				if (!allCombos.contains(s2)) {
					allCombos.add(s2);
				}
			}
			else if (sameDigits) {
				String s = "";
				for (int k = 0; k < str.length; k++) {
					s = s + Integer.toString(str[k]);
				}
				if (!allCombos.contains(s)) {
					allCombos.add(s);
				}
			}
			else {
				int m = str.length; 
				permute(str, 0, m - 1,allCombos);
			}
		}

		ArrayList<String> goodCombos = new ArrayList<String>();

		for (int l = 0; l < allCombos.size(); l++) {
			boolean add = true;
			String s = allCombos.get(l);
			if (s.contains("1")) {
				if (numberOfOnes == 0) {
					if (s.charAt(s.length()-1) != '1') {
						add = false;
					}
				}
				else {
					int spNum = 0;
					ArrayList<Integer> posOnes = new ArrayList<Integer>();
					for (int m = 0; m < s.length(); m++) {
						int thisSpNum = s.charAt(m);
						spNum = spNum + thisSpNum;
						if (thisSpNum == '1') {
							posOnes.add(spNum);
						}
						if (!ones.contains(spNum)) {
							add = false;
						}
					}
				}
			}

			if (add) {
				ArrayList<StarSection> ssections = new ArrayList<StarSection>();

				for (Map.Entry<Integer, StarSection> entry : starMap.entrySet()) {
					StarSection ss = entry.getValue();
					ssections.add(ss);
				}

				int currentSp = 0;
				int totalSp = 0;

				for (int p = 0; p < s.length(); p++) {
					int x = Character.getNumericValue(s.charAt(p));
					currentSp = x;
					totalSp = totalSp + x;
					double sp = 0.0;
					int active = 0 ;

					for (int y = totalSp - currentSp; y < totalSp; y++ ) {
						StarSection ss = ssections.get(y);
						if (!lastSyncEvent) {
							lastSyncIndex = updateSync(ss.getTime(),lastSyncIndex);
						}


						double lv = ss.returnLength();

						sp = sp + ss.getMeasures() + lv;
						if (sp > 8) {
							sp = 8;
						}

						active = getActivationNote(ss.getTime(), ss.getLength());
						SortedMap<Integer,Note> subMap = noteMap.subMap(active,noteMap.lastKey());
						active = subMap.firstKey();
					}


					if (!lastSyncEvent) {
						lastSyncIndex = updateSync(active,lastSyncIndex);
					}

					double tsMes = ts.getTop() / ts.getBottom() * 4;
					int splength = (int) Math.round(tsMes * sp * resolution);
					int end = active + splength;

					if (!lastSyncEvent) {
						end = checkSync(active,end,sp);
					}

					int check = checkExtraSPNoMod(active,end,sp);

					if (check != 0) {
						//System.out.println("Overlap");
						add = false;
					}
				}
			}

			if (add) {
				goodCombos.add(s);
			}
		}

		for (int a = 0; a < goodCombos.size(); a++) {
			CList.add(goodCombos.get(a));
		}		
	}

	public void findCombinationsUtil(int arr[], ArrayList<int[]> arr2, int index, 
			int num, int reducedNum) 
	{ 		
		if (reducedNum < 0) 
			return; 

		if (reducedNum == 0) 
		{ 
			int tempArr[] = new int[index];
			int numOf1 = 0;

			for (int i = 0; i < index; i++) {
				if (arr[i]==1) {
					numOf1++;
				}
				if (arr[i]!=0) {
					tempArr[i] = arr[i];
				}
			}

			if (numOf1 <= numberOfOnes + 1) {
				arr2.add(tempArr);
			}

			return; 
		} 

		int prev = (index == 0) ? 
				1 : arr[index - 1]; 

		for (int k = prev; k <= num ; k++) 
		{ 
			arr[index] = k; 
			findCombinationsUtil(arr, arr2, index + 1, num, 
					reducedNum - k); 
		} 
	}

	public void permute(int[] str, int l, int r, ArrayList<String> list) 
	{ 
		if (l == r) {
			String a = "";
			for (int j=0;j<str.length;j++) {
				//System.out.print(str[j] + " ");
				a = a + str[j];
			}
			if (!list.contains(a))
				list.add(a);
			//System.out.println("");
		}
		else { 
			for (int i = l; i <= r; i++) { 
				swap(str, l, i); 
				permute(str, l + 1, r, list); 
				swap(str, l, i); 
			} 
		} 
	} 

	/** 
	 * Swap Characters at position 
	 * @param a string value 
	 * @param i position 1 
	 * @param j position 2 
	 * @return swapped string 
	 */
	public void swap(int[] a, int i, int j) 
	{ 
		int temp; 
		temp = a[i]; 
		a[i] = a[j]; 
		a[j] = temp; 
	} 


	public int checkSync (int active, int end, double sp) {

		double tsMes = ts.getTop() / ts.getBottom() * 4;
		int splength = (int) Math.round(tsMes * sp * resolution);

		ArrayList<SyncEvent> syncChange = new ArrayList<SyncEvent>();

		for (int i = lastSyncIndex; i < syncEvents.size(); i++) {
			int s = syncEvents.get(i).getInd();
			if (s > active && s < end){
				syncChange.add(syncEvents.get(i));
			}
			if (s > end) {
				break;
			}
		}

		if (syncChange.size() > 0) {

			ArrayList<Integer> splengthsplit = new ArrayList<Integer>();
			int spsplit;
			int begin = active;

			for (int i = 0; i < syncChange.size(); i++) {
				if (i > 0) {
					begin = syncChange.get(i-1).getInd();
				}
				spsplit = syncChange.get(i).getInd() - begin;
				double meselapsed = spsplit / resolution / tsMes;
				double newsp = sp - meselapsed;
				if (i==0) {
					splengthsplit.add(spsplit);
				}
				lastSyncIndex = updateSync(syncChange.get(i).getInd(),lastSyncIndex);
				int spl = (int) Math.round(tsMes * newsp * resolution);
				splengthsplit.add(spl);
			}

			splength = 0;

			for (int i=0; i < splengthsplit.size(); i++) {
				splength = splength + splengthsplit.get(i);
			}

			return active + splength;
		}

		else {
			return end;
		}


	}

	public int checkExtraSP (int active, int end, double sp) {
		boolean extraSP = true;
		int exists = 0;
		while (extraSP) {
			SortedMap<Integer,StarSection> subMap = starMap.subMap(active, end);
			if (subMap.size() > 0) {
				for (Map.Entry<Integer, StarSection> entry : subMap.entrySet())  {
					StarSection ssn = entry.getValue();
					int t = ssn.getTime();
					int l = ssn.getLength();

					int spsplit = t - active;
					double tsMes = ts.getTop() / ts.getBottom() * 4;
					double meselapsed = spsplit / resolution / tsMes;
					double newsp = sp - meselapsed;

					double rl = ssn.returnLength();

					if (noWhammy) {
						rl = 0;
					}

					if (rl > 0) {
						SortedMap<Integer,Note> nlMap = noteMap.subMap(active, end);
						ArrayList<Note> nlList = new ArrayList<Note>();
						double spused = 0.0;

						for (Map.Entry<Integer, Note> entry3 : nlMap.entrySet())  {
							Note n = entry3.getValue();
							if (n.getLength() > 0) {
								nlList.add(n);
							}
						}

						for (int i = 0; i < nlList.size(); i++) {
							double newsp2 = spLeft (active, nlList.get(i).getTime(), newsp);

							if (newsp2 == 0) {
								ssn.setMeasures(ssn.getMeasures() - spused);
								starMap.replace(entry.getKey(), ssn);
								extraSP = false;
							} 
							else {
								double lv2 = nlList.get(i).getLength() / resolution / 3.75;
								newsp = newsp + lv2;
								if (newsp > 8) {
									newsp = 8;
								}
								int splength2 = (int) Math.round(tsMes * newsp * resolution);
								end = nlList.get(i).getTime() + splength2;
								end = checkSync(nlList.get(i).getTime(),end,newsp);
								spused = spused + lv2;
							}
						}

						newsp = newsp + 2;
						if (newsp > 8) {
							newsp = 8;
						}
						int splength2 = (int) Math.round(tsMes * newsp * resolution);
						end = t + l + splength2;
						end = checkSync(t,end,newsp);
						ssn.setMeasures(-100);
						exists++;
						starMap.replace(entry.getKey(), ssn);
					}
					else if (t + l < end) {
						newsp = newsp + 2;
						if (newsp > 8) {
							newsp = 8;
						}
						int splength2 = (int) Math.round(tsMes * newsp * resolution);
						end = t + l + splength2;
						end = checkSync(t,end,newsp);
						ssn.setMeasures(-100);
						exists++;
						starMap.replace(entry.getKey(), ssn);
					}
					else {
						extraSP = false;
					}
				}
				extraSP = false;
			}
			else {
				extraSP = false;
			}
		}

		return exists;
	}

	public int checkExtraSPNoMod (int active, int end, double sp) {
		boolean extraSP = true;
		int exists = 0;
		while (extraSP) {
			SortedMap<Integer,StarSection> subMap = starMap.subMap(active, end);
			if (subMap.size() > 0) {
				for (Map.Entry<Integer, StarSection> entry : subMap.entrySet())  {
					StarSection ssn = entry.getValue();
					int t = ssn.getTime();
					int l = ssn.getLength();

					int spsplit = t - active;
					double tsMes = ts.getTop() / ts.getBottom() * 4;
					double meselapsed = spsplit / resolution / tsMes;
					double newsp = sp - meselapsed;

					double rl = ssn.returnLength();

					if (noWhammy) {
						rl = 0;
					}

					if (rl > 0) {
						SortedMap<Integer,Note> nlMap = noteMap.subMap(active, end);
						ArrayList<Note> nlList = new ArrayList<Note>();
						//double spused = 0.0;

						for (Map.Entry<Integer, Note> entry3 : nlMap.entrySet())  {
							Note n = entry3.getValue();
							if (n.getLength() > 0) {
								nlList.add(n);
							}
						}

						for (int i = 0; i < nlList.size(); i++) {
							double newsp2 = spLeft (active, nlList.get(i).getTime(), newsp);

							if (newsp2 == 0) {
								extraSP = false;
							} 
							else {
								double lv2 = nlList.get(i).getLength() / resolution / 3.75;
								newsp = newsp + lv2;
								if (newsp > 8) {
									newsp = 8;
								}
								int splength2 = (int) Math.round(tsMes * newsp * resolution);
								end = nlList.get(i).getTime() + splength2;
								end = checkSync(nlList.get(i).getTime(),end,newsp);
								//spused = spused + lv2;
							}
						}

						newsp = newsp + 2;
						if (newsp > 8) {
							newsp = 8;
						}
						int splength2 = (int) Math.round(tsMes * newsp * resolution);
						end = t + l + splength2;
						end = checkSync(t,end,newsp);
						//ssn.setMeasures(-100);
						exists++;
						//starMap.replace(entry.getKey(), ssn);
					}
					else if (t + l < end) {
						newsp = newsp + 2;
						if (newsp > 8) {
							newsp = 8;
						}
						int splength2 = (int) Math.round(tsMes * newsp * resolution);
						end = t + l + splength2;
						end = checkSync(t,end,newsp);
						//ssn.setMeasures(-100);
						exists++;
						//starMap.replace(entry.getKey(), ssn);
					}
					else {
						extraSP = false;
					}
				}
				extraSP = false;
			}
			else {
				extraSP = false;
			}
		}

		return exists;
	}

	public ArrayList<Integer> CheckOnes () {
		ArrayList<Integer> ones = new ArrayList<Integer>();
		int i = 1;
		
		for (Map.Entry<Integer, StarSection> entry : starMap.entrySet())  {
			StarSection ss = entry.getValue();
			double measures = ss.getMeasures() + ss.returnLength();
			if (measures >= 4.0) {
				ones.add(i);
			}
			i++;
		}

		return ones;
	}

	public class TimeSig {

		int top = 4;
		int bottom = 4;

		public TimeSig () {}

		public TimeSig (int t, int b) {
			this.top = t;
			this.bottom = b;
		}

		public int getTop() {
			return top;
		}

		public void setTop(int top) {
			this.top = top;
		}

		public int getBottom() {
			return bottom;
		}

		public void setBottom(int bottom) {
			this.bottom = bottom;
		}

	}

	public class SyncEvent {

		int ind = 0;
		String type = "";
		int value = 0;
		int value2 = 0;

		public SyncEvent () {}

		public SyncEvent (String i, String t, String v) {
			this.ind = Integer.parseInt(i);
			this.type = t;
			if (v.contains(" ")) {
				String topBottom[] = v.split(" ");
				this.value = Integer.parseInt(topBottom[0]);
				this.value2 = Integer.parseInt(topBottom[1]);
			}
			else {
				this.value = Integer.parseInt(v);
			}
		}

		public SyncEvent (int i, String t, int v) {
			this.ind = i;
			this.type = t;
			this.value = v;
		}

		public int getInd() {
			return ind;
		}

		public void setInd(int ind) {
			this.ind = ind;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public int getValue() {
			return value;
		}
		
		public int getValue2() {
			return value2;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}

	public class Note {

		int time = 0;
		String fret = "";
		int length = 0;
		int value = 0;

		public Note (int t, String f, String l, int v) {
			this.time = t;
			this.fret = f;
			this.length = Integer.parseInt(l);
			this.value = v;
		}

		public void chord(String f, int v) {
			this.fret = this.fret + f;
			this.value = this.value + v;
		}

		public void chord(Note n) {
			this.fret = n.getFret() + this.fret;
			this.value =  n.getValue() + this.value;
		}

		public int getTime() {
			return time;
		}

		public void setTime(int time) {
			this.time = time;
		}

		public String getFret() {
			return fret;
		}

		public void setFret(String fret) {
			this.fret = fret;
		}

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

	}

	public class StarSection {

		int time = 0;
		int length = 0;
		double measures = 2.0;

		public StarSection (int t, int l) {
			this.time = t;
			this.length = l;
		}

		public double addLength () {
			//Need to figure out way to adjust if bpm or ts changes in middle of hold note
			int notesLength = getLengthSum(time, time+length);
			double lv = 0.0;

			if (notesLength > 0) {
				lv = (double) notesLength / resolution;
				lv = lv / 3.75;
				this.measures = this.measures + lv;
			}

			return lv;
		}

		public double returnLength () {
			//Need to figure out way to adjust if bpm or ts changes in middle of hold note
			int notesLength = getLengthSum(time, time+length);
			double lv = 0.0;

			if (notesLength > 0) {
				lv = (double) notesLength / resolution;
				lv = lv / 3.75;
			}

			return lv;
		}

		public int getTime() {
			return time;
		}

		public void setTime(int time) {
			this.time = time;
		}

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public double getMeasures() {
			return measures;
		}

		public void setMeasures(double measures) {
			this.measures = measures;
		}

	}

	public static void main(String[] args) {
		StarPather test = new StarPather();

		File chart = new File("C:/Users/tmerwitz/Downloads/notes (4).chart");
		InputStream is = null;

		try {
			is = new FileInputStream(chart);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		test.parseFile(is);
		test.printStarMap();
		test.noSqueezePath();
		System.out.println(test.getOutput());
		//System.out.println("Ultra Easy Score: " + test.ultraEasyPath());
		//System.out.println("Ultra Easy NW Score: " + test.ultraEasyNoWhammy());
		//System.out.println("Ultra Easy Full SP Score: " + test.ultraEasyFullPath());
	}	

}
package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
	private double bestSp;
	private int lastBestScore;
	int soloBonus = 0;

	protected String name = "";
	private StringBuffer output = new StringBuffer();

	private int resolution = 192;
	private double bpm = 0.0;
	private double bps = 0.0;
	private double posEarly = 0.0;
	private double posLate = 0.0;
	public boolean takeFromNext = false;
	private double spFromNext = 0.0;
	private boolean containsTen = false;

	private TimeSig ts = new TimeSig();

	private ArrayList<SyncEvent> syncEvents = new ArrayList<SyncEvent>();
	private ArrayList<SoloSection> SoloSections = new ArrayList<SoloSection>();

	private boolean lastSyncEvent = false;
	private int lastSyncIndex = 0;
	//private SyncEvent current = new SyncEvent();
	//private SyncEvent next = new SyncEvent();

	SortedMap<Integer,SyncEvent> syncMap = new TreeMap<Integer,SyncEvent>();
	SortedMap<Integer,Note> noteMap = new TreeMap<Integer,Note>();
	SortedMap<Integer,StarSection> starMap = new TreeMap<Integer,StarSection>();

	private boolean noWhammy = false;
	private boolean badWhammy = false;
	private boolean lazyWhammy = false;
	private boolean earlyWhammy = false;
	private boolean squeeze = false;

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
	
	public void setNoWhammy(boolean b) {
		this.noWhammy = b;
	}
	
	public void setBadWhammy(boolean b) {
		this.badWhammy = b;
	}
	
	public void setLazyWhammy(boolean b) {
		this.lazyWhammy = b;
	}
	
	public void setEarlyWhammy(boolean b) {
		this.earlyWhammy = b;
	}
	public void setSqueeze(boolean b) {
		this.squeeze = b;
	}
	public void setTakeNext(boolean b) {
		this.takeFromNext = b;
	}
	public void setNextSp(double i) {
		this.spFromNext = i;
	}
	public void setContainsTen(boolean b) {
		this.containsTen = b;
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
					posEarly = Math.ceil(bps * .065 * resolution);
					posLate = Math.ceil(bps * .075 * resolution);
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
			
			int lv = (int) Math.ceil(((lengthDif / resolution) * (resolution / Math.ceil(resolution/25))));
			sum = sum - lv;
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
		int offset = 2;

		StringBuffer songInfo = new StringBuffer();
		
		int soloB = 0;
		int soloE = 0;

		try{
			in = new BufferedReader(new InputStreamReader(chart));

			while ((theline = in.readLine()) != null){
				if(theline == null || theline.trim().length()==0) {
					continue;
				}
				this.linesRead++;
				this.lastLine = theline;

				if (theline.contains("[Song]")) {
					songSection = true;
					continue;
				}

				if (songSection) {
					if (theline.startsWith("{")) {
						continue;
					}
					else if (theline.contains("Name = ")) {
						offset = theline.indexOf("=") - 5;
						String songName = theline.substring(offset+7).trim().replace("\"", "");
						appendName(songName);
						songInfo.append(songName);
						continue;
					}
					else if (theline.startsWith("Artist =")) {
						String artistName = " by " + theline.substring(offset+9).trim().replace("\"", "");
						appendName(artistName);
						songInfo.append(artistName);
						continue;
					}
					else if (theline.startsWith("Resolution =") || theline.startsWith("  Resolution =")) {
						resolution = Integer.parseInt(theline.substring(offset+13).trim());
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
						String ind = theline.substring(offset,split).trim();
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

						int time = Integer.parseInt(theline.substring(offset,split).trim());

						if (!lastSyncEvent) {
							lastSyncIndex = updateSync(time,lastSyncIndex);
						}

						if (theline.charAt(split+2) == 'N') {

							String fret = theline.substring(split+4,split+6).trim();

							if (fret.equals("6") || fret.equals("5")) {
								continue;
							}
							else {
								String length = theline.substring(split+6).trim();
								int value = 50;

								/*if (!length.equals("0")) {
									double lv = Double.parseDouble(length) / resolution;
									lv = lv * 25;
									value = (int) (value + Math.round(lv));
								}*/

								if (notes == 0) {
									Note n = new Note(time,fret,length,value);
									noteMap.put(time, n);
									notes++;
									continue;
								}
								
								Note lastnote = noteMap.get(noteMap.lastKey());

								if (noteMap.containsKey(time)) {
									if (notes == 9 || notes == 19 || notes == 29) {
										mult--;
									}
									value = value * mult;
									if (notes == 9 || notes == 19 || notes == 29) {
										mult++;
									}
									Note n = new Note(time,fret,length,value);
									n.chord(noteMap.get(time));
									noteMap.replace(time, n);
									continue;
								}
								else {
									if (!maxMult) {
										if (notes >= 29) {
											mult = 4;
											maxMult = true;
										}
										else if (notes >= 19 && mult < 3) {
											mult = 3;
										}
										else if (notes >= 9 && mult < 2) {
											mult = 2;
										}
									}
									
									value = value * mult;
									
									if (time < lastnote.getTime() + lastnote.getLength()) {
										int thisEnd = time + Integer.parseInt(length);
										int lastEnd = lastnote.getTime() + lastnote.getLength();
										int dif = thisEnd - lastEnd;
										if (dif < 0) {
											dif = 0;
										}
										length = "" + dif;
									}
									
									if (!length.equals("0")) {
										/*double lv = Double.parseDouble(length) / resolution;
										lv = lv * mult * (resolution / (resolution/25));
										value = (int) (value + lv);*/
										
										int lv = (int) Math.ceil(((Double.parseDouble(length) / resolution) * (resolution / Math.ceil(resolution/25))) * mult);
										value = value + lv;
									}					

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
						
						else if (theline.substring(split+1).equals(" E solo")) {
							soloB = Integer.parseInt(theline.substring(offset,split).trim());
						}
						
						else if (theline.contains("= E soloend")) {
							soloE = Integer.parseInt(theline.substring(offset,split).trim()) + 1;
							SoloSection soS = new SoloSection (soloB,soloE);
							SoloSections.add(soS);
							soloB = 0;
							soloE = 0;
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
			if (soloB != 0 && soloE == 0) {
				soloE = noteMap.lastKey();
				SoloSection soS = new SoloSection (soloB,soloE);
				SoloSections.add(soS);
			}
			
			int score = getTotalSum();
			output.append("Notes: " + notes + "\n");
			output.append("Base Score: " + score + "\n");
			if (squeeze) {
				double extra = (posEarly + posLate) / resolution;
				//output.append("Extra measures from squeezing: " + extra);
			}
			
			if (SoloSections.size() > 0) {
				for (int i = 0; i < SoloSections.size(); i++) {
					SoloSection sos = SoloSections.get(i);
					SortedMap<Integer,Note> soloMap = noteMap.subMap(sos.getBegin(),sos.getEnd());
					soloBonus = soloBonus + (soloMap.size() * 100);
				}
				output.append("Solo Bonuses: " + soloBonus + "\n");
				int bs = score + soloBonus;
				output.append("Base Score plus Solos: " + bs + "\n");
			}
			output.append("\n");
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
			
			double lv = 0;

			if (ss.getMeasures() > 0) {
				lv = ss.returnLength();
			}
			sp = sp + ss.getMeasures() + lv;
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
				pathScore = pathScore + spSum  + soloBonus;
				sp = 0;
			}
		}
		output.append("Easy Score: " + pathScore);
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

			double lv = 0;

			if (ss.getMeasures() > 0) {
				lv = ss.returnLength();
			}
			sp = sp + ss.getMeasures() + lv;
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
				pathScore = pathScore + spSum + soloBonus;
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
			int lastSpLength = 0;

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
				
				boolean skipPath = false;
				setTakeNext(false);
				boolean testAnother = false;
				setNextSp(0.0);
				String tempBestDetail = "";
				String firstTest = "";
				int testFirstAct = 0;
				int testLastAct = 0;
				int firstMapStart = 0;
				ArrayList<StarSection> tempSpValues = new ArrayList<StarSection>();
				int tempMaxSpLength = 0;
				
				lastBestScore = 0;

				for (int j = 0; j < s.length(); j++) {
					int x = Character.getNumericValue(s.charAt(j));
					currentSp = x;
					totalSp = totalSp + x;
					double sp = 0.0;
					int active = 0 ;
					double nextSub = spFromNext;
					//bestScore = 0;
					ArrayList<Integer> activations = new ArrayList<Integer>();
					ArrayList<StarSection> spValues = new ArrayList<StarSection>();
					boolean secondLast = false;

					for (int k = totalSp - currentSp; k < totalSp; k++ ) {
						StarSection ss = ssections.get(k);
						if (!lastSyncEvent) {
							lastSyncIndex = updateSync(ss.getTime(),lastSyncIndex);
						}

						double lv = ss.returnLength();
						if (takeFromNext) {
							testAnother = true;
							lv = lv - (spFromNext/3.75);
							setTakeNext(false);
						}
						sp = sp + ss.getMeasures() + lv;

						if (sp < 4 && currentSp == 1 && totalSp == ssections.size()) {
							j = s.length();
							k = totalSp;
							sp = 0;
						}
						
						spValues.add(ss);

						/*if (sp > 8) {
							sp = 8;
						}*/

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
					int sqLen = 0;
					if (squeeze) {
						sqLen = (int) (posEarly + posLate);
					}
					splength = splength + sqLen;
					int maxsplength = (int) Math.round(tsMes * 8 * resolution);
					maxsplength = maxsplength + sqLen;
					int firstActive = active;
					int lastActive = 0;

					if (totalSp < ssections.size()) {
						StarSection nextSs = ssections.get(totalSp);
						int nextSsActive = getActivationNote(nextSs.getTime(), nextSs.getLength());
						SortedMap<Integer,Note> subMap2 = noteMap.subMap(firstActive,nextSsActive-1);
						lastActive = subMap2.lastKey() - splength;
						if (firstActive == lastActive) {
							lastActive++;
						}
						else if (firstActive > lastActive) {
							skipPath = true;
							continue;
						}
						else {
							SortedMap<Integer,Note> subMap3 = noteMap.subMap(firstActive,lastActive);
							lastActive = subMap3.lastKey();
						}
					}
					else if (sp > 0){
						int last = noteMap.lastKey();
						Note lastNote = noteMap.get(last);
						lastActive = lastNote.getTime() - splength;
						SortedMap<Integer,Note> subMap4 = noteMap.subMap(lastActive,noteMap.lastKey());
						if (!subMap4.isEmpty())
							lastActive = subMap4.firstKey();
					}
					
					int bestActivation = getHighestScore(firstActive,lastActive,splength,maxsplength,spValues);
					
					if (takeFromNext) {
						splength = (int) (splength + spFromNext);
						double spFromCheck = spFromNext;
						int bestActivation2 = getHighestScore(bestActivation,lastActive,splength,maxsplength,spValues);
						while (spFromCheck < spFromNext) {
							spFromCheck = spFromNext - 1;
							bestActivation2 = getHighestScore(bestActivation2,lastActive,splength,maxsplength,spValues);
							if (spFromNext - 1 == spFromCheck) {
								spFromCheck++;
							}
						}
						bestActivation = bestActivation2;
					}

					if (activations.size()==0) {
						continue;
					}
					int lastStar = activations.get(activations.size()-1);
					String activeNumber = "";
					if (bestActivation < lastStar) {
						for (int m = 0; m < activations.size(); m++) {
							if (bestActivation < activations.get(m)) {
								activeNumber = m + "(" + (currentSp - m) + ")";
								m = activations.size();
							}
						}
					}
					else {
						activeNumber = "" + currentSp;
					}
					//double mesDis = (1.0 * splength) /( 1.0 * resolution)/tsMes;
					String activeDetail = activeNumber + " (" + (bestSp/resolution/tsMes) + " SP)\n";
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
						activeDetail = activeDetail + fretCounter(subMap6,noteFret,activeBefore,onNote) + "\n";
						
						//pathDetail.append(activeDetail+"\n");
						
						if (squeeze) {
							SortedMap<Integer,Note> subMap7 = noteMap.subMap(activeNote.getTime()+1,activeNote.getTime()+splength);
							noteFret = subMap7.get(subMap7.lastKey()).getFret();
							activeDetail = activeDetail + "Squeeze " +fretCounter(subMap7,noteFret,0,onNote) + "\n";
							//pathDetail.append("Squeeze " + activeDetail +"\n");
						}
						
						activeDetail = activeDetail + "\n";
						//boolean perserve = takeFromNext;
						//int lastBestPre = lastBestScore;
						
						/*if (testAnother) {
							//tempBestDetail = tempBestDetail + activeDetail;
							int currentBestCombo = bestScore + lastBestScore;
							
							int testBest1 = getHighestScore(testFirstAct,testLastAct,lastSpLength,tempMaxSpLength,tempSpValues);
							splength = (int) (splength + nextSub);
							int testBest2 = getHighestScore(firstActive,lastActive,splength,maxsplength,spValues);
							int testCombo = bestScore + lastBestScore;
							
							if (testCombo > currentBestCombo) {
								//mesDis = (1.0 * lastSpLength) /( 1.0 * resolution)/tsMes;
								tempBestDetail = firstTest + "\n";
								activeNote = noteMap.get(testBest1);
								activeBefore = 0;
								onNote = true;
								if (activeNote == null) {
									SortedMap<Integer,Note> subMap51 = noteMap.subMap(testBest1,noteMap.lastKey());
									activeNote = subMap51.get(subMap51.firstKey());
									onNote = false;
									activeBefore = activeNote.getTime() - testBest1;
								}
								afterStar = Integer.parseInt(Character.toString(firstTest.charAt(0)));
								SortedMap<Integer,Note> subMap10 = noteMap.subMap(firstMapStart+1,activeNote.getTime()+1);
								noteFret = activeNote.getFret();
								tempBestDetail = tempBestDetail + fretCounter(subMap10,noteFret,activeBefore,onNote) + "\n";
								
								if (squeeze) {
									SortedMap<Integer,Note> subMap11 = noteMap.subMap(activeNote.getTime()+1,activeNote.getTime()+lastSpLength);
									noteFret = subMap11.get(subMap11.lastKey()).getFret();
									tempBestDetail = tempBestDetail + "Squeeze " +fretCounter(subMap11,noteFret,0,onNote) + "\n\n";
									//pathDetail.append("Squeeze " + activeDetail +"\n");
								}
								
								pathDetail.append(tempBestDetail);
								
								if (actualPath.length() != 0) {
									actualPath = actualPath + ", ";
								}
								actualPath = actualPath + firstTest;
								comboScore = comboScore + lastBestScore;
								
								//mesDis = (1.0 * splength) /( 1.0 * resolution)/tsMes;
								activeDetail = activeNumber + "\n";
								activeNote = noteMap.get(testBest2);
								activeBefore = 0;
								onNote = true;
								if (activeNote == null) {
									SortedMap<Integer,Note> subMap52 = noteMap.subMap(testBest2,noteMap.lastKey());
									activeNote = subMap52.get(subMap52.firstKey());
									onNote = false;
									activeBefore = activeNote.getTime() - testBest2;
								}
								afterStar = Integer.parseInt(Character.toString(activeNumber.charAt(0)));
								SortedMap<Integer,Note> subMap12 = noteMap.subMap(mapStart+1,activeNote.getTime()+1);
								noteFret = activeNote.getFret();
								activeDetail = activeDetail + fretCounter(subMap12,noteFret,activeBefore,onNote) + "\n";
								
								if (squeeze) {
									SortedMap<Integer,Note> subMap13 = noteMap.subMap(activeNote.getTime()+1,activeNote.getTime()+lastSpLength);
									noteFret = subMap13.get(subMap13.lastKey()).getFret();
									activeDetail = activeDetail + "Squeeze " +fretCounter(subMap13,noteFret,0,onNote) + "\n\n";
									//pathDetail.append("Squeeze " + activeDetail +"\n");
								}
							}
							
							else {
								setTakeNext(perserve);
								comboScore = comboScore + lastBestPre;
								if (actualPath.length() != 0) {
									actualPath = actualPath + ", ";
								}
								actualPath = actualPath + firstTest;
								pathDetail.append(tempBestDetail);
							}
							
							tempBestDetail = "";
							testFirstAct = 0;
							lastSpLength =  0;
							firstTest =  "";
							firstMapStart =  0;
							testAnother = false;
							maxsplength = 0;
							tempSpValues.clear();
						}
						
						if (takeFromNext && ssections.size() > totalSp) {
							tempBestDetail = tempBestDetail + activeDetail;
							testFirstAct = firstActive;
							lastSpLength = splength;
							firstTest = activeNumber;
							firstMapStart = mapStart;
							tempSpValues.clear();
							for (int ti = 0; ti < spValues.size(); ti++) {
								tempSpValues.add(spValues.get(ti));
							}
							tempMaxSpLength = maxsplength;
							
							StarSection nextSs = ssections.get(totalSp);
							int firstLen = 0;
							if (nextSs.getTime() >= activeNote.getTime()+splength) {
								setTakeNext(false);
							}
							if (takeFromNext) {
								SortedMap<Integer,Note> subMap8 = noteMap.subMap(nextSs.getTime(),activeNote.getTime()+splength);
								for (Map.Entry<Integer, Note> entry : subMap8.entrySet()) {
									Note nn = entry.getValue();
									if (firstLen == 0 && nn.getLength() > 0) {
										firstLen = nn.getTime();
									}
								}
								if (firstLen == 0) {
									takeFromNext = false;
								}
								else {
									SortedMap<Integer,Note> subMap99 = noteMap.subMap(nextSs.getTime(),firstLen+1);
									testLastAct = subMap99.get(subMap99.lastKey()).getTime() - splength;
								}
							}
						}*/
						pathDetail.append(activeDetail);
						if (actualPath.length() != 0) {
							actualPath = actualPath + ", ";
						}
						actualPath = actualPath + activeNumber;
						comboScore = comboScore + bestScore;
						
						
					}
				}
				if (skipPath) {
					continue;
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
			pathScore = pathScore + bestComboScore + soloBonus;
			output.append("Best Path: " + bestPath + "\n");
			output.append("Score = " + pathScore + "\n\n");
			output.append(bestPathDetail);
			return pathScore;
		} catch (Exception e) {
			
			e.printStackTrace();
			return 0;
		}

	}
	
	public String fretCounter (SortedMap<Integer,Note> subMap6, String noteFret, int activeBefore, boolean onNote) {

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

		return activeDetail;
	
	}

	public int getHighestScore (int firstAct, int lastAct, int sp, int maxsp, ArrayList<StarSection> sss) {
		int activationPoint = 0;
		int highestScore = 0;
		double highestSp = 0;
		setTakeNext(false);
		setNextSp(0);

		for (int i = firstAct; i <= lastAct; i++) {
			double maxcheck = 0;
			double nextcheck = 0;
			Note nnn = null;
			int fullSp = 0;
			for (int j = 0; j < sss.size(); j++) {
				StarSection jss = sss.get(j);
				SortedMap<Integer,Note> subMap2 = noteMap.subMap(jss.getTime(),jss.getTime()+jss.getLength());
				Note jnn = subMap2.get(subMap2.lastKey());
				if (jnn.getTime() <= i) {
					maxcheck = maxcheck + jss.getMeasures() + jss.returnLength();
				}
				else {
					nextcheck = jss.returnLength();
					if (nextcheck == 0) {
						nextcheck = nextcheck + .01;
					}
					nnn = jnn;
					j = sss.size();
				}
			}
			if (maxcheck >= 8.0) {
				fullSp = maxsp;
			}
			else if (nextcheck > 0) {
				if (nextcheck == .01) {
					nextcheck = nextcheck - .01;
				}
				nextcheck = spLeft (i,nnn.getTime(),maxcheck+nextcheck);
				if (nextcheck > 6.0) {
					nextcheck = nextcheck - 6.0;
					double tsMes = ts.getTop() / ts.getBottom() * 4;
					int splength = (int) Math.round(tsMes * nextcheck * resolution);
					fullSp = sp - splength;
				}
				else if ( nextcheck < 0) {
					continue;
				}
			} 
			else {
				fullSp = sp;
			}
			
			if (nnn!= null) {
				int max = nnn.getTime() + maxsp;
				if (i+fullSp > max) {
					fullSp = max - i;
				}
			}
			
			int sectionScore = getValueSum(i,i+fullSp);
			if (sectionScore > highestScore) {
				SortedMap<Integer,StarSection> subMap = starMap.subMap(i,i+fullSp);
				if (subMap.size() >= 1 && nnn==null) {
					StarSection ss = subMap.get(subMap.firstKey());
					if (ss.returnLength() > 0) {
						int sLen = getLengthSum(ss.getTime(),i+fullSp);
						if (sLen > 0) {
							setTakeNext(true);
							double nsp;
							nsp = (double) sLen / resolution;
							nsp = nsp / 3.75;
							setNextSp(nsp);
						}
					}
				}
				highestScore = sectionScore;
				highestSp = fullSp;
				activationPoint = i;
			}
			else if (sectionScore == highestScore) {
				Note note = noteMap.get(i);
				Note note2 = noteMap.get(activationPoint);
				if (note2 == null && note != null) {

					SortedMap<Integer,StarSection> subMap = starMap.subMap(i,i+fullSp);
					if (subMap.size() >= 1 && nnn==null) {
						StarSection ss = subMap.get(subMap.firstKey());
						if (ss.returnLength() > 0) {
							int sLen = getLengthSum(ss.getTime(),i+fullSp);
							if (sLen > 0) {
								setTakeNext(true);
								double nsp;
								nsp = (double) sLen / resolution;
								nsp = nsp / 3.75;
								setNextSp(nsp);
							}
						}
					}
					activationPoint = i;
					highestSp = fullSp;
				
				}
			}
		}

		lastBestScore = bestScore;
		bestScore = highestScore;
		bestSp = highestSp;
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
		
		if (n >= 10) {
			ArrayList<int[]> tempList = new ArrayList<int[]>();
			for (int in = 0; in < comboList.size(); in++) {
				int[] temp = comboList.get(in);
				boolean add = true;
				for (int jn = 0; jn < temp.length; jn++) {
					int t = temp[jn];
					if (n > 15 && t >= 5) {
						add = false;
						jn = temp.length;
					}
					if (n > 30 && t >= 4) {
						add = false;
						jn = temp.length;
					}
				}
				if (add) {
					tempList.add(comboList.get(in));
				}
			}
			
			comboList.clear();
			
			for (int kn = 0; kn < tempList.size(); kn++) {
				comboList.add(tempList.get(kn));
			}
		} 

		ArrayList<String> allCombos = new ArrayList<String>();

		for (int i = 0; i < comboList.size(); i++) {
			int[] str = new int[comboList.get(i).length];
			int digit = 0;
			boolean sameDigits = true;
			boolean samePlusOnes = true;
			/*if (starMap.size() > 9) {
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
			}*/
			for (int j = 0; j < str.length; j++) {
				str[j] = comboList.get(i)[j];
				if (str[j] != 1 && digit == 0) {
					digit = str[j];
				}
				if (j > 0) {
					if (str[j] != digit || str[j] == 1) {
						sameDigits = false;
					}
					if (str[j] != digit && str[j] != 1) {
						samePlusOnes = false;
					}
				}
			}
			boolean excessOnes = false;
			int checkOnesCount = 0;
			for (int checkOnes = 0; checkOnes < str.length; checkOnes++) {
				int checker = str[checkOnes];
				if (checker == 1)
					checkOnesCount++;
				if (checkOnesCount > numberOfOnes + 1) {
					excessOnes = true;
					checkOnes = str.length;
				}
			}
			if (excessOnes) {
				continue;
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
			else if (samePlusOnes) {
				if (numberOfOnes == 0 || numberOfOnes == 1) {
					String oneCombo = "";
					for (int iones = 0; iones < str.length; iones++) {
						if (iones != str.length-1) {
							oneCombo = oneCombo + digit;
						}
						else {
							oneCombo = oneCombo + "1";
						}
					}
					if (!allCombos.contains(oneCombo)) {
						allCombos.add(oneCombo);
					}
				}
				else if (checkOnesCount == numberOfOnes + 1) {
					String s = "";
					for (int strl = 0; strl < str.length; strl++) {
						s = s + str[strl];
					}
					StringBuilder sb = new StringBuilder(s);
					boolean addThis = true;
					for (int oner = 0; oner < ones.size(); oner++) {
						if (ones.get(oner) >= sb.length()) {
							addThis = false;
							continue;
						}
						sb.setCharAt(ones.get(oner)-1, '1');
					}
					if (!addThis) {
						continue;
					}
					sb.setCharAt(sb.length()-1, '1');
					s = sb.toString();
					if (!allCombos.contains(s)) {
						allCombos.add(s);
					}
				}
				else if (checkOnesCount == numberOfOnes) {
					String s = "";
					for (int strl = 0; strl < str.length; strl++) {
						s = s + str[strl];
					}
					StringBuilder sb = new StringBuilder(s);
					boolean addThis = true;
					for (int oner = 0; oner < ones.size(); oner++) {
						if (ones.get(oner) >= sb.length()) {
							addThis = false;
							continue;
						}
						sb.setCharAt(ones.get(oner)-1, '1');
					}
					if (!addThis) {
						continue;
					}
					s = sb.toString();
					if (!allCombos.contains(s)) {
						allCombos.add(s);
					}
				}
				else if (str.length < 8){
					QuickPerm(str,allCombos);
				}
				else {
					LongPerm(str,allCombos);
				}
			}
			else if (str.length < 8) {
				QuickPerm(str,allCombos);
			}
			else {
				LongPerm(str,allCombos);
			}
		}

		//System.out.println("Total combos: " + allCombos.size());
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
					if (arr[i] >= 10) {
						setContainsTen(true);
					}
				}
			}

				if (!containsTen)
					arr2.add(tempArr);
				else
					setContainsTen(false);
				
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
	
	public void QuickPerm(int[] combo, ArrayList<String> all)
	{
	   //System.out.println(all.size());
		int n = combo.length;
	   int a[] = new int[n];
	   int p[] = new int[n+1];
	   int i, j, tmp; // Upper Index i; Lower Index j

	   for(i = 0; i < n; i++)   // initialize arrays; a[N] can be any type
	   {
	      a[i] = combo[i];   // a[i] value is not revealed and can be arbitrary
	      p[i] = i;
	   }
	   p[n] = n; // p[N] > 0 controls iteration and the index boundary for i
	   quickCheck(a, 0, 0, all,n);   // remove comment to display array a[]
	   i = 1;   // setup first swap points to be 1 and 0 respectively (i & j)
	   while(i < n)
	   {
	      p[i]--;             // decrease index "weight" for i by one
	      j = i % 2 * p[i];   // IF i is odd then j = p[i] otherwise j = 0
	      tmp = a[j];         // swap(a[j], a[i])
	      a[j] = a[i];
	      a[i] = tmp;
	      quickCheck(a, j, i,all,n); // remove comment to display target array a[]
	      i = 1;              // reset index i to 1 (assumed)
	      while (p[i] == 0)       // while (p[i] == 0)
	      {
	         p[i] = i;        // reset p[i] zero value
	         i++;             // set new index value for i (increase by one)
	      } // while(!p[i])
	   } // while(i < N)
	} // QuickPerm()
	
	void quickCheck(int a[], int j, int i, ArrayList<String> all, int n)            
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

	
	public void LongPerm(int[] combo, ArrayList<String> all) {
		int div = combo.length / 5;
		int rem = combo.length % div;
		int len = combo.length / div;
		
		int [][] ray = new int[div][];
		
		for (int i = 0; i < ray.length; i++) {
			int le = len;
			int re = 0;
			if (rem!= 0 && i == ray.length - 1) {
				le = le + rem;
				re = rem;
			}
			ray[i] = new int [le];
			int it = 0;
			for (int j = i * len; j < (i+1) * len + re; j++) {
				ray[i][it] = combo[j];
				it++;
			}
		}
		
		ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
		
		for (int k = 0; k < ray.length; k++) {
			int digit = 0;
			boolean sameDigits = true;
			
			if (list.size() < k + 1) {
				list.add(new ArrayList<String>());
			}
			/*if (starMap.size() > 9) {
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
			}*/
			for (int m = 0; m < ray[k].length; m++) {
				if (digit == 0) {
					digit = ray[k][m];
				}
				if (m > 0) {
					if (ray[k][m] != digit) {
						sameDigits = false;
					}
				}
			}
			
			if (sameDigits) {
				String s = "";
				for (int p = 0; p < ray[k].length; p++) {
					s = s + Integer.toString(ray[k][p]);
				}
				if (!list.get(k).contains(s)) {
					list.get(k).add(s);
				}
			}
			
			else {
				QuickPerm(ray[k], list.get(k));
			}
		}
		
		if (list.size() == 2) {
			for (int q = 0; q < list.get(0).size(); q++) {
				String s1 = list.get(0).get(q);
				for (int r = 0; r < list.get(1).size(); r++) {
					String s2 = list.get(1).get(r);
					String com = s1 + s2;
					if (!all.contains(com)) {
						all.add(com);
					}
					com = s2 + s1;
					if (!all.contains(com)) {
						all.add(com);
					}
				}
			}
		}
		
		if (list.size() == 3) {
			for (int q = 0; q < list.get(0).size(); q++) {
				String s1 = list.get(0).get(q);
				for (int r = 0; r < list.get(1).size(); r++) {
					String s2 = list.get(1).get(r);
					for (int s = 0; s < list.get(2).size(); s++) {
						String s3 = list.get(2).get(s);
						String com = s1 + s2 + s3;
						if (!all.contains(com)) {
							all.add(com);
						}
						com = s2 + s1 + s3;
						if (!all.contains(com)) {
							all.add(com);
						}
						com = s3 + s1 + s2;
						if (!all.contains(com)) {
							all.add(com);
						}
						com = s1 + s3 + s2;
						if (!all.contains(com)) {
							all.add(com);
						}
						com = s2 + s3 + s1;
						if (!all.contains(com)) {
							all.add(com);
						}
						com = s3 + s2 + s1;
						if (!all.contains(com)) {
							all.add(com);
						}
					}
				}
			}
		}
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
	
	public void FastPerm(String str,String prefix,ArrayList<String> all)
	{
	    if(str.length()==0)
	    {
	    	if (!all.contains(prefix))
	    		all.add(prefix);
	    }
	    else
	    {
	        for (int i = 0; i<str.length();i++)
	        {
	            if(i>0)
	            {
	                if(str.charAt(i)==str.charAt(i-1))
	                {
	                    continue;
	                }
	            }

	            FastPerm(str.substring(0, i)+str.substring(i+1, str.length()),prefix+str.charAt(i), all);

	        }
	    }
	}
	
	public void test () {
		File outfile = new File("Combos.txt");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(outfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 16; i < 17; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append("case " + i + ":\n");
			int arr[] = new int [i];
			ArrayList<int[]> comboList = new ArrayList<int[]>();
			ArrayList<String> allCombos = new ArrayList<String>();
			SortedMap<Integer,ArrayList<String>> sepCombos = new TreeMap<Integer,ArrayList<String>>();

			findCombinationsUtil(arr,comboList,0,i,i);
			
			for (int j = 0; j < comboList.size(); j++) {
				int[] str = new int[comboList.get(j).length];
				int digit = 0;
				boolean sameDigits = true;
				if (i > 9) {
					boolean skip = false;
					for (int q = 0; q < comboList.get(j).length; q++) {
						int checkOverTen = comboList.get(j)[q];
						if (checkOverTen >= 10) {
							skip = true;
						} 
					}
					if (skip) {
						continue;
					}
				}
				for (int jc = 0; jc < str.length; jc++) {
					str[jc] = comboList.get(j)[jc];
					if (digit == 0) {
						digit = str[jc];
					}
					if (jc > 0) {
						if (str[jc] != digit) {
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
					String s = "";
					for (int k = 0; k < str.length; k++) {
						s = s + Integer.toString(str[k]);
					}
					FastPerm(s,"",allCombos);
				}
			}
			
			for (int k = 0; k < allCombos.size(); k++) {
				String s = allCombos.get(k);
				
				int oneCount = 0;

			    for(int l=0; l < s.length(); l++)
			    {    if(s.charAt(l) == '1')
			            oneCount++;
			    }
			    
			    oneCount--;
			    
			    if (oneCount < 0) {
			    	if (!sepCombos.containsKey(0)) {
			    		sepCombos.put(0,new ArrayList<String>());
			    	}
			    	sepCombos.get(0).add(s);
			    }
			    else if (oneCount == 0 && s.charAt(s.length()-1) == '1'){
			    	if (!sepCombos.containsKey(0)) {
			    		sepCombos.put(0,new ArrayList<String>());
			    	}
			    	sepCombos.get(0).add(s);
			    }
			    else {
			    	if (s.charAt(s.length()-1) != '1')
			    		oneCount ++;
			    	if (!sepCombos.containsKey(oneCount)) {
			    		sepCombos.put(oneCount,new ArrayList<String>());
			    	}
			    	sepCombos.get(oneCount).add(s);
			    }
			}
			
			for (Map.Entry<Integer, ArrayList<String>> entry : sepCombos.entrySet())  {
				if (entry.getValue().size() == 0) {
					continue;
				}
				if (entry.getKey() > 0) {
					sb.append("if (ones > " + (entry.getKey() - 1) + ") {\n");
				}
				
				for (int n = 0; n < entry.getValue().size(); n++) {
					sb.append("list.add(\"" + entry.getValue().get(n) + "\");\n");
				}
				
				if (entry.getKey() > 0) {
					sb.append("}\n");
				}
			}
			sb.append("break;\n");
			try {
				out.write(sb.toString().getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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

		public double returnLength () {
			//Need to figure out way to adjust if bpm or ts changes in middle of hold note
			int notesLength = getLengthSum(time, time+length);
			double lv = 0.0;

			if (notesLength > 0) {
				SortedMap<Integer,Note> nlMap = noteMap.subMap(time, time+length);
				int nn = 0;
				if (posEarly == 0) {
					if (!lastSyncEvent) {
						lastSyncIndex = updateSync(time,lastSyncIndex);
					}
				}
				for (Map.Entry<Integer, Note> entry : nlMap.entrySet())  {
					Note n = entry.getValue();
					if (n.length>0) {
						nn++;
					}
				}
				double nnn = nn * posEarly;
				if (earlyWhammy) {
					notesLength = (int) (notesLength + nnn);
				}
				lv = (double) notesLength / resolution;
				lv = lv / 3.75;
			}
			
			double whammy = 1.0;
			
			if (noWhammy) {
				whammy = 0;
			}
			else if (badWhammy) {
				whammy = 0.5;
			}
			else if (lazyWhammy) {
				whammy = 0.8;
			}

			return lv * whammy;
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
	
	public class SoloSection {
		int begin = 0;
		int end = 0;
		
		public SoloSection (int b, int e) {
			this.begin = b;
			this.end = e;
		}
		
		public int getBegin() {
			return begin;
		}

		public void setBegin(int begin) {
			this.begin = begin;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}
	}

	public static void main(String[] args) {
		StarPather test = new StarPather();

		File chart = new File("C:/Users/tmerwitz/Downloads/Overflow_test (1).chart");
		InputStream is = null;

		try {
			is = new FileInputStream(chart);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//test.test();

		test.setEarlyWhammy(true);
		test.setSqueeze(true);
		//test.setLazyWhammy(true);
		test.parseFile(is);
		test.printStarMap();
		//ArrayList<String> combos = new ArrayList<String>();
		//test.checkStarCombos(combos);
		//test.ultraEasyFullPath();
		test.noSqueezePath();
		System.out.println(test.getOutput());
		//System.out.println("Ultra Easy Score: " + test.ultraEasyPath());
		//System.out.println("Ultra Easy NW Score: " + test.ultraEasyNoWhammy());
		//System.out.println("Ultra Easy Full SP Score: " + test.ultraEasyFullPath());
	}	

}

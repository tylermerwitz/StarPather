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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
	private int bestSpLength = 0;
	int soloBonus = 0;
	DecimalFormat df = new DecimalFormat("#.##");

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
	private boolean sixFret = false;

	private TimeSig ts = new TimeSig();

	private ArrayList<SyncEvent> syncEvents = new ArrayList<SyncEvent>();
	private ArrayList<SoloSection> SoloSections = new ArrayList<SoloSection>();

	private boolean lastSyncEvent = false;
	private int lastSyncIndex = 0;
	//private SyncEvent current = new SyncEvent();
	//private SyncEvent next = new SyncEvent();

	SortedMap<Integer,SyncEvent> tsMap = new TreeMap<Integer,SyncEvent>();
	SortedMap<Integer,SyncEvent> bpmMap = new TreeMap<Integer,SyncEvent>();
	SortedMap<Integer,Note> noteMap = new TreeMap<Integer,Note>();
	SortedMap<Integer,StarSection> starMap = new TreeMap<Integer,StarSection>();

	private boolean noWhammy = false;
	private boolean badWhammy = false;
	private boolean lazyWhammy = false;
	private boolean earlyWhammy = false;
	private boolean squeeze = false;
	private String instrument = "ExpertSingle";

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
	public void setBestSpLength (int i) {
		bestSpLength = i;
	}

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	/*--updateSync--
	 *This method is used to update the BPM and time signature
	 *at a given point so that the program can correctly calculate
	 *both the length of a SP activation and the amount of SP gained
	 *while whammying sustains.
	 *The "time" parameter must be entered as the chart position.
	 *Doing so will update the global variables 'ts' (time signature),
	 *BPM, BPS, posEarly (used for squeezing and early whammying),
	 *and posLate (used for squeezing)*/
	public void updateSync (int time) {
		if (time <= 0) { //This check was added to catch a specific bug, need to test deletion
			time = 1;
		}
		SortedMap<Integer,SyncEvent> subMap = tsMap.subMap(tsMap.firstKey(), time);
		SyncEvent e = subMap.get(subMap.lastKey()); //Finds the last SyncEvent that occurs before the position entered

		ts.setTop(e.getValue());
		/*A .chart file lists time signatures in a convoluted manner
		 * where the bottom of a time sig reads as follows:
		 * 0 = 4, 3 = 8, 4 = 16
		 * Logic would dictate that 5 = 32 and 6 = 64
		 * but I have yet to come across a chart that uses a 32 or 64 TS
		 * and I would need an example of one in order to confirm this and
		 * add the code to handle such.*/
		if (e.getValue2() == 0)
			ts.setBottom(4);
		else if (e.getValue2() == 3)
			ts.setBottom(8);
		else if (e.getValue2() == 4)
			ts.setBottom(16);
	
		SortedMap<Integer,SyncEvent> subMap2 = bpmMap.subMap(bpmMap.firstKey(), time);
		SyncEvent e2 = subMap2.get(subMap2.lastKey());

		bpm = e2.getValue()/1000;
		bps = bpm/60;
		posEarly = Math.ceil(bps * .065 * resolution);
		posLate = Math.ceil(bps * .075 * resolution);
		/*According to the sources I could find, the standard hit
		 * windows in CH are 65 ms early and 75 ms late when the
		 * video offset is equal to 0. Therefore BPM dictates the
		 * positional windows for squeezing and early whammying.*/


	}

	//Old version of updateSync, now deprecated
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

	/*--getTotalSum--
	 * This method returns the total score of the chart BEFORE solos and
	 * any star power activations are added, AKA the "BASE SCORE"*/
	public int getTotalSum () {
		int sum = 0;

		for (Map.Entry<Integer, Note> entry : noteMap.entrySet())  {
			Note n = entry.getValue();
			sum = sum + n.getValue();
		}

		return sum;
	}

	/*--getValuesSum--
	 * This method returns the total values of notes between two positions.
	 * This is the main method used for obtaining the highest score for
	 * a given activation. To do so, you enter the activation position (min)
	 * and the position in which Star Power will run out (max)*/
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
		if (max < noteEnd) { //Needed in case SP ends on sustain note
			int lengthDif = noteEnd - max;

			int lv = (int) Math.ceil(((lengthDif / resolution) * (resolution / Math.ceil(resolution/25))));
			sum = sum - lv;
		}

		SortedMap<Integer,Note> subMap2 = noteMap.subMap(noteMap.firstKey(), min);
		if (subMap2.size() > 0) {
			Note ln = subMap2.get(subMap2.lastKey());
			noteEnd = ln.getTime() + ln.getLength();
			if (noteEnd > min) { //Needed in case SP begins on sustain note
				int lengthDif = noteEnd - min;

				int lv = (int) Math.ceil(((lengthDif / resolution) * (resolution / Math.ceil(resolution/25))));
				sum = sum + lv;
			}
		}

		return sum;
	}

	/*--getLengthSum--
	 * Returns the total length of all sustain notes between
	 * two positions. This method is mostly used to calculate
	 * the amount of whammy SP can be gained in a given SP
	 * phrase.*/
	public int getLengthSum (int min, int max) {
		int sum = 0;

		SortedMap<Integer,Note> subMap = noteMap.subMap(min, max);

		for (Map.Entry<Integer, Note> entry : subMap.entrySet())  {
			Note n = entry.getValue();
			sum = sum + n.getLength();
		}

		return sum;
	}

	/*--getInstruments--
	 * */
	public void getInstruments (InputStream chart, HashMap<String,ArrayList<String>> list) {
		BufferedReader in = null;
		String theline    = null;

		try {
			in = new BufferedReader(new InputStreamReader(chart));

			while ((theline = in.readLine()) != null){
				if(theline == null || theline.trim().length()==0) {
					continue;
				}

				String inst = "";

				if (theline.contains("Single]")) {
					inst = "Lead Guitar";
					if (!list.isEmpty()) {
						if (!list.containsKey("Lead Guitar")) {
							ArrayList<String> l = new ArrayList<String>();
							list.put("Lead Guitar",l);
						}
					}
					else {
						ArrayList<String> l = new ArrayList<String>();
						list.put("Lead Guitar",l);
					}
				}

				if (theline.contains("DoubleGuitar]")) {
					inst = "Co-Op Guitar";
					if (!list.isEmpty()) {
						if (!list.containsKey("Co-Op Guitar")) {
							ArrayList<String> l = new ArrayList<String>();
							list.put("Co-Op Guitar",l);
						}
					}
					else {
						ArrayList<String> l = new ArrayList<String>();
						list.put("Co-Op Guitar",l);
					}
				}

				if (theline.contains("DoubleBass]")) {
					inst = "Bass";
					if (!list.isEmpty()) {
						if (!list.containsKey("Bass")) {
							ArrayList<String> l = new ArrayList<String>();
							list.put("Bass",l);
						}
					}
					else {
						ArrayList<String> l = new ArrayList<String>();
						list.put("Bass",l);
					}
				}

				if (theline.contains("DoubleRhythm]")) {
					inst = "Rhythm Guitar";
					if (!list.isEmpty()) {
						if (!list.containsKey("Rhythm Guitar")) {
							ArrayList<String> l = new ArrayList<String>();
							list.put("Rhythm Guitar",l);
						}
					}
					else {
						ArrayList<String> l = new ArrayList<String>();
						list.put("Rhythm Guitar",l);
					}
				}

				if (theline.contains("Keyboard]")) {
					inst = "Keyboard";
					if (!list.isEmpty()) {
						if (!list.containsKey("Keyboard")) {
							ArrayList<String> l = new ArrayList<String>();
							list.put("Keyboard",l);
						}
					}
					else {
						ArrayList<String> l = new ArrayList<String>();
						list.put("Keyboard",l);
					}
				}

				if (theline.contains("GHLGuitar]")) {
					inst = "6 Fret Guitar";
					if (!list.isEmpty()) {
						if (!list.containsKey("6 Fret Guitar")) {
							ArrayList<String> l = new ArrayList<String>();
							list.put("6 Fret Guitar",l);
						}
					}
					else {
						ArrayList<String> l = new ArrayList<String>();
						list.put("6 Fret Guitar",l);
					}
				}

				if (theline.contains("GHLBass]")) {
					inst = "6 Fret Bass";
					if (!list.isEmpty()) {
						if (!list.containsKey("6 Fret Bass")) {
							ArrayList<String> l = new ArrayList<String>();
							list.put("6 Fret Bass",l);
						}
					}
					else {
						ArrayList<String> l = new ArrayList<String>();
						list.put("6 Fret Bass",l);
					}
				}

				if (theline.contains("[Expert")) {
					if (!list.get(inst).contains("Expert")) {
						list.get(inst).add("Expert");
					}
				}

				if (theline.contains("[Hard")) {
					if (!list.get(inst).contains("Hard")) {
						list.get(inst).add("Hard");
					}
				}

				if (theline.contains("[Medium")) {
					if (!list.get(inst).contains("Medium")) {
						list.get(inst).add("Medium");
					}
				}

				if (theline.contains("[Easy")) {
					if (!list.get(inst).contains("Easy")) {
						list.get(inst).add("Easy");
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
			if (chart !=null) {
				try {
					chart.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

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
						songInfo.append(songName + "\n");
						continue;
					}
					else if (theline.contains("Artist =")) {
						String artistName = "by " + theline.substring(offset+9).trim().replace("\"", "");
						appendName(artistName);
						songInfo.append(artistName + "\n");
						continue;
					}
					else if (theline.contains("Charter =")) {
						String artistName = "Charted by " + theline.substring(offset+10).trim().replace("\"", "");
						appendName(artistName);
						songInfo.append(artistName + "\n");
						continue;
					}
					else if (theline.startsWith("Resolution =") || theline.startsWith("  Resolution =")) {
						resolution = Integer.parseInt(theline.substring(offset+13).trim());
						//System.out.println(resolution);
						continue;
					}
					else if (theline.startsWith("}")) {
						songInfo.append("\n");
						songSection = false;
						continue;
					}
					/*else {
						theline = theline.trim();
						songInfo.append(theline);
						continue;
					}*/
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
						if (type.equals("TS")) {
							tsMap.put(Integer.parseInt(ind), e);
						}
						else {
							bpmMap.put(Integer.parseInt(ind), e);
						}
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

				if (theline.startsWith("[" + getInstrument() +  "]")) {
					notesSection = true;
					if (theline.startsWith("[ExpertGHLGuitar]") || theline.startsWith("[ExpertGHLBass]")) {
						sixFret = true;
					}
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

						if (tsMap.size() > 0 && bpmMap.size() > 0)
							updateSync(time);

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
			output.append(songInfo);
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

			if (chart !=null) {
				try {
					chart.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
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
				int splength = (int) Math.ceil(tsMes * sp * resolution);
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
				int splength = (int) Math.ceil(tsMes * sp * resolution);
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

	public String bestPathEver () {
		String orDetail = getOutput();
		if (starMap.size() < 23 || starMap.size() > 70) {
			noSqueezePath();
			return getOutput();
		}
		else {
			int n = starMap.size();
			ArrayList<ArrayList<Integer>> comboList = new ArrayList<ArrayList<Integer>>();

			checkTensCombos(comboList,n);

			int bestScore = 0;
			String bestDetail = "";

			for (int i = 0; i < comboList.size(); i++) {
				int score = noSqueezePathAlt(comboList.get(i));
				String currentDetail = getOutput();

				if (score > bestScore) {
					bestScore = score;
					bestDetail = currentDetail;
				}
			}

			String detail = orDetail + bestDetail;

			return detail;
		}
	}

	public int noSqueezePathAlt (ArrayList<Integer> comList) {

		try {
			output.replace(0, output.length()-1, "");

			int pathScore = getTotalSum();

			int totalBestComboScore = 0;
			String totalBestPath = "";
			String totalBestPathDetail = "";

			ArrayList<StarSection> sssections = new ArrayList<StarSection>();
			for (Map.Entry<Integer, StarSection> entry : starMap.entrySet()) {
				StarSection ss = entry.getValue();
				sssections.add(ss);
			}

			ArrayList<SortedMap<Integer,StarSection>> starSubMaps = new ArrayList<SortedMap<Integer,StarSection>>();

			int total = 0;
			for (int smi = 0; smi < comList.size(); smi++) {
				int current = comList.get(smi);
				total = total + current;
				starSubMaps.add(new TreeMap<Integer,StarSection>());
				for (int smj = total - current; smj < total - 1; smj++) {
					StarSection ss = sssections.get(smj);
					starSubMaps.get(smi).put(ss.getTime(), ss);
				}
			}


			for (int smk = 0; smk < starSubMaps.size(); smk++) {

				int bestComboScore = 0;
				String bestPath = "";
				String bestPathDetail = "";

				SortedMap<Integer,StarSection> starSub = starSubMaps.get(smk);

				int nextStarMapSection = 0;

				if (smk + 1 < starSubMaps.size()) {
					SortedMap<Integer,StarSection> subMap = starMap.subMap(starSub.lastKey()+1,starMap.lastKey());
					nextStarMapSection = subMap.firstKey();
				}

				ArrayList<StarSection> ssections = new ArrayList<StarSection>();
				for (Map.Entry<Integer, StarSection> entry : starSub.entrySet()) {
					StarSection ss = entry.getValue();
					ssections.add(ss);
				}

				ArrayList<String> combos = new ArrayList<String>();
				checkStarCombos(combos,starSub);

				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				System.out.println("Permutation " + smk + " finished at: " + dateFormat.format(date));

				SortedMap<String,Activation> acts = new TreeMap<String,Activation>();

				for (int i = 0; i < combos.size(); i++) {
					String s = combos.get(i);

					int currentSp = 0;
					int totalSp = 0;
					int comboScore = 0;

					String actualPath = "";
					StringBuffer pathDetail = new StringBuffer();
					//String pathHeader = "Path " + (i+1) + ": " + s + "\n";

					boolean skipPath = false;
					setTakeNext(false);
					setNextSp(0.0);
					//ArrayList<StarSection> tempSpValues = new ArrayList<StarSection>();
					//int tempMaxSpLength = 0;

					lastBestScore = 0;

					for (int j = 0; j < s.length(); j++) {
						int x = Character.getNumericValue(s.charAt(j));
						currentSp = x;
						totalSp = totalSp + x;
						String key = totalSp + "-" + currentSp;
						double sp = 0.0;
						int active = 0 ;
						setBestSpLength(0);
						//bestScore = 0;
						ArrayList<Integer> activations = new ArrayList<Integer>();
						ArrayList<StarSection> spValues = new ArrayList<StarSection>();
						boolean secondLast = false;

						if (takeFromNext) {
							key = key + "-Minus" + spFromNext;
						}

						int current = 0;

						if (acts.containsKey(key)) {
							Activation a = acts.get(key);
							pathDetail.append(a.activeDetail);
							if (actualPath.length() != 0 || totalBestPath.length() != 0) {
								actualPath = actualPath + ", ";
							}
							actualPath = actualPath + a.activeNumber;
							comboScore = comboScore + a.getScore();

							setTakeNext(a.takeFromNext);
							setNextSp(a.spFromNext);
						}

						else {
							for (int k = totalSp - currentSp; k < totalSp; k++ ) {
								StarSection ss = ssections.get(k);

								updateSync(ss.getTime());

								double lv = ss.returnLength();
								if (takeFromNext) {
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

									updateSync(firstAct);

									double currentTsMes = ts.getTop() / ts.getBottom() * 4;
									int currentSplength = (int) Math.ceil(currentTsMes * sp * resolution);

									SortedMap<Integer,Note> subMap = noteMap.subMap(firstAct,secondAct);
									active = subMap.lastKey() - currentSplength;
									current++;
									int truesplength = calcSpLengthNoError(active,sp,currentSp,current);
									if (truesplength <= 0) {
										truesplength = currentSplength;
									}
									active = subMap.lastKey() - truesplength;
									secondLast = true;
								}
								else if (!secondLast){
									if (sp < 6 ) {
										current++;
										active = getActivationNote(ss.getTime(),ss.getLength());
										SortedMap<Integer,Note> subMap = noteMap.subMap(active,noteMap.lastKey());
										active = subMap.firstKey();
									}
									else {

									}
								}
							}

							if (sp == 0) {
								continue;
							}

							updateSync(active);

							double tsMes = 1.0 * ts.getTop() / ts.getBottom() * 4;
							int splength = (int) Math.ceil(tsMes * sp * resolution);
							int truesplength = calcSpLengthNoError(active,sp,currentSp,current);
							int sqLen = 0;
							if (squeeze) {
								sqLen = (int) (posEarly + posLate);
							}
							if (truesplength <= 0) {
								truesplength = splength;
								truesplength = truesplength + sqLen;
							}
							int maxsplength = (int) Math.ceil(tsMes * 8 * resolution);
							maxsplength = maxsplength + sqLen;
							int firstActive = active;
							int lastActive = 0;

							/*if (totalSp < ssections.size()) {
							StarSection nextSs = ssections.get(totalSp);
							int nextSsActive = getActivationNote(nextSs.getTime(), nextSs.getLength());
							SortedMap<Integer,Note> subMap2 = noteMap.subMap(firstActive,nextSsActive-1);
							lastActive = subMap2.lastKey() - truesplength;
							if (firstActive == lastActive) {
								lastActive++;
							}
							else if (firstActive > lastActive) {
								skipPath = true;
								continue;
							}
							else {
								SortedMap<Integer,Note> subMap3 = noteMap.subMap(firstActive,lastActive);
								if (subMap3.size() > 0) {
									lastActive = subMap3.lastKey();
								}
							}
						}
						else if (sp > 0){
							int last = noteMap.lastKey();
							Note lastNote = noteMap.get(last);
							lastActive = lastNote.getTime() - truesplength;
							if (j == s.length() - 1 && nextStarMapSection!= 0) {
								lastActive = nextStarMapSection - truesplength;
							}
							if (firstActive == lastActive) {
								lastActive++;
							}
							else if (firstActive > lastActive) {
								skipPath = true;
								continue;
							}
							else
							 {
								SortedMap<Integer,Note> subMap4 = noteMap.subMap(lastActive,noteMap.lastKey());
								if (!subMap4.isEmpty())
									lastActive = subMap4.firstKey();
							 }
						}*/

							firstActive = findFirstAct(spValues);

							if (firstActive == -1) {
								skipPath = true;
								continue;
							}

							lastActive = findLastAct(spValues);

							if (firstActive == lastActive) {
								lastActive++;
							}
							else if (firstActive > lastActive) {
								skipPath = true;
								continue;
							}

							Activation a = getHighestScore(firstActive,lastActive,truesplength,maxsplength,spValues);

							if (a.takeFromNext) {
								truesplength = (int) (truesplength + a.getSpFromNext());
								double spFromCheck = a.getSpFromNext();
								Activation a2 = getHighestScore(a.begin,lastActive,truesplength,maxsplength,spValues);
								while (spFromCheck < a2.getSpFromNext()) {
									spFromCheck = a2.getSpFromNext() - 1;
									a2 = getHighestScore(a2.begin,lastActive,truesplength,maxsplength,spValues);
									if (a2.getSpFromNext() - 1 == spFromCheck) {
										spFromCheck++;
									}
								}
								a = a2;
							}

							if (activations.size()==0) {
								continue;
							}
							int lastStar = activations.get(activations.size()-1);
							String activeNumber = "";
							if (a.begin < lastStar) {
								for (int m = 0; m < activations.size(); m++) {
									if (a.begin < activations.get(m)) {
										activeNumber = m + "(" + (currentSp - m) + ")";
										m = activations.size();
									}
								}
							}
							else {
								activeNumber = "" + currentSp;
							}
							a.setActiveNumber(activeNumber);
							int acti = Integer.parseInt(activeNumber.substring(0,1));
							double actiMeasures = 0.0;
							for (int act = 0; act < acti; act++) {
								actiMeasures = actiMeasures + spValues.get(act).getMeasures() + spValues.get(act).returnLength();
							}
							actiMeasures = Math.round(actiMeasures / 8 * 100);
							if (actiMeasures > 100) {
								actiMeasures = 100;
							}
							//double mesDis = (1.0 * splength) /( 1.0 * resolution)/tsMes;
							String activeDetail = activeNumber + " (" + df.format(actiMeasures) + "% SP)\n";
							//pathDetail.append(bestActivation+"\n");

							Note activeNote = noteMap.get(a.begin);
							int afterStar = Integer.parseInt(Character.toString(activeNumber.charAt(0)));

							boolean onNote = true;
							int activeBefore = 0;
							if (activeNote == null) {
								SortedMap<Integer,Note> subMap5 = noteMap.subMap(a.begin,noteMap.lastKey());
								activeNote = subMap5.get(subMap5.firstKey());
								onNote = false;
								activeBefore = activeNote.getTime() - a.begin;
							}

							if (afterStar != 0) {
								int mapStart = activations.get(afterStar - 1);
								SortedMap<Integer,Note> subMap6 = noteMap.subMap(mapStart+1,activeNote.getTime()+1);
								String noteFret = activeNote.getFret();
								activeDetail = activeDetail + fretCounter(subMap6,noteFret,activeBefore,onNote) + 
										" (Beat " + df.format(1.0 * a.begin/resolution) + ")\n";

								//pathDetail.append(activeDetail+"\n");

								if (squeeze && a.end > 0) {
									SortedMap<Integer,Note> subMap7 = noteMap.subMap(activeNote.getTime()+1,a.end);
									if (subMap7.size() > 0) {
										String noteFret2 = subMap7.get(subMap7.lastKey()).getFret();
										activeDetail = activeDetail + "Squeeze " +fretCounter(subMap7,noteFret2,0,onNote) +
												" (Beat " + df.format(1.0 * subMap7.lastKey()/resolution) + ")\n";
										//pathDetail.append("Squeeze " + activeDetail +"\n");
									}
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
								a.setActiveDetail(activeDetail);
								pathDetail.append(activeDetail);
								if (actualPath.length() != 0 || smk > 0) {
									actualPath = actualPath + ", ";
								}
								actualPath = actualPath + activeNumber;
								comboScore = comboScore + a.getScore();

								setTakeNext(a.takeFromNext);
								setNextSp(a.spFromNext);

								acts.put(key,a);
							}
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
						//bestPathLiteral = s;
						bestPathDetail = pathDetail.toString();
					}
				}

				totalBestComboScore = totalBestComboScore + bestComboScore;
				totalBestPath = totalBestPath + bestPath;
				totalBestPathDetail = totalBestPathDetail + bestPathDetail;
			}
			pathScore = pathScore + totalBestComboScore + soloBonus;
			output.append("Best Path: " + totalBestPath + "\n");
			output.append("Score = " + pathScore + "\n\n");
			output.append(totalBestPathDetail);
			return totalBestComboScore;
		}
		catch (Exception e) {

			e.printStackTrace();
			return 0;
		}

	}

	public int noSqueezePath () {

		try {
			int pathScore = getTotalSum();

			int totalBestComboScore = 0;
			String totalBestPath = "";
			String totalBestPathDetail = "";

			ArrayList<StarSection> sssections = new ArrayList<StarSection>();
			for (Map.Entry<Integer, StarSection> entry : starMap.entrySet()) {
				StarSection ss = entry.getValue();
				sssections.add(ss);
			}

			ArrayList<SortedMap<Integer,StarSection>> starSubMaps = new ArrayList<SortedMap<Integer,StarSection>>();

			if (starMap.size() > 20) {
				int d = starMap.size() / 10;
				int r = starMap.size() % d ;
				int n = starMap.size() / d;

				for (int smi = 0; smi < d; smi++) {
					int range = 0;
					if (smi + 1 == d) {
						range = range + r;
					}
					starSubMaps.add(new TreeMap<Integer,StarSection>());
					for (int smj = smi * n; smj < ((smi + 1) * n) + range; smj++) {
						StarSection ss = sssections.get(smj);
						starSubMaps.get(smi).put(ss.getTime(), ss);
					}
				}
			}
			else {
				starSubMaps.add(starMap);
			}

			for (int smk = 0; smk < starSubMaps.size(); smk++) {

				int bestComboScore = 0;
				String bestPath = "";
				String bestPathDetail = "";

				SortedMap<Integer,StarSection> starSub = starSubMaps.get(smk);

				int nextStarMapSection = 0;

				if (smk + 1 < starSubMaps.size()) {
					SortedMap<Integer,StarSection> subMap = starMap.subMap(starSub.lastKey()+1,starMap.lastKey());
					nextStarMapSection = subMap.firstKey();
				}

				ArrayList<StarSection> ssections = new ArrayList<StarSection>();
				for (Map.Entry<Integer, StarSection> entry : starSub.entrySet()) {
					StarSection ss = entry.getValue();
					ssections.add(ss);
				}

				ArrayList<String> combos = new ArrayList<String>();
				checkStarCombos(combos,starSub);

				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				System.out.println("Permutation " + smk + " finished at: " + dateFormat.format(date));

				SortedMap<String,Activation> acts = new TreeMap<String,Activation>();

				for (int i = 0; i < combos.size(); i++) {
					String s = combos.get(i);

					int currentSp = 0;
					int totalSp = 0;
					int comboScore = 0;

					String actualPath = "";
					StringBuffer pathDetail = new StringBuffer();
					//String pathHeader = "Path " + (i+1) + ": " + s + "\n";

					boolean skipPath = false;
					setTakeNext(false);
					setNextSp(0.0);
					//ArrayList<StarSection> tempSpValues = new ArrayList<StarSection>();
					//int tempMaxSpLength = 0;

					lastBestScore = 0;

					for (int j = 0; j < s.length(); j++) {
						int x = Character.getNumericValue(s.charAt(j));
						currentSp = x;
						totalSp = totalSp + x;
						String key = totalSp + "-" + currentSp;
						double sp = 0.0;
						int active = 0 ;
						setBestSpLength(0);
						//bestScore = 0;
						ArrayList<Integer> activations = new ArrayList<Integer>();
						ArrayList<StarSection> spValues = new ArrayList<StarSection>();
						boolean secondLast = false;

						if (takeFromNext) {
							key = key + "-Minus" + spFromNext;
						}

						int current = 0;

						if (acts.containsKey(key)) {
							Activation a = acts.get(key);
							pathDetail.append(a.activeDetail);
							if (actualPath.length() != 0 || totalBestPath.length() != 0) {
								actualPath = actualPath + ", ";
							}
							actualPath = actualPath + a.activeNumber;
							comboScore = comboScore + a.getScore();

							setTakeNext(a.takeFromNext);
							setNextSp(a.spFromNext);
						}

						else {
							for (int k = totalSp - currentSp; k < totalSp; k++ ) {
								StarSection ss = ssections.get(k);
								updateSync(ss.getTime());

								double lv = ss.returnLength();
								if (takeFromNext) {
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

									updateSync(firstAct);

									double currentTsMes = ts.getTop() / ts.getBottom() * 4;
									int currentSplength = (int) Math.ceil(currentTsMes * sp * resolution);

									SortedMap<Integer,Note> subMap = noteMap.subMap(firstAct,secondAct);
									active = subMap.lastKey() - currentSplength;
									current++;
									int truesplength = calcSpLengthNoError(active,sp,currentSp,current);
									if (truesplength <= 0) {
										truesplength = currentSplength;
									}
									active = subMap.lastKey() - truesplength;
									secondLast = true;
								}
								else if (!secondLast){
									if (sp < 6 ) {
										current++;
										active = getActivationNote(ss.getTime(),ss.getLength());
										SortedMap<Integer,Note> subMap = noteMap.subMap(active,noteMap.lastKey());
										active = subMap.firstKey();
									}
									else {

									}
								}
							}

							if (sp == 0) {
								continue;
							}

							updateSync(active);

							double tsMes = 1.0 * ts.getTop() / ts.getBottom() * 4;
							int splength = (int) Math.ceil(tsMes * sp * resolution);
							int truesplength = calcSpLengthNoError(active,sp,currentSp,current);
							int sqLen = 0;
							if (squeeze) {
								sqLen = (int) (posEarly + posLate);
							}
							if (truesplength <= 0) {
								truesplength = splength;
								truesplength = truesplength + sqLen;
							}
							int maxsplength = (int) Math.ceil(tsMes * 8 * resolution);
							maxsplength = maxsplength + sqLen;
							int firstActive = active;
							int lastActive = 0;

							/*if (totalSp < ssections.size()) {
							StarSection nextSs = ssections.get(totalSp);
							int nextSsActive = getActivationNote(nextSs.getTime(), nextSs.getLength());
							SortedMap<Integer,Note> subMap2 = noteMap.subMap(firstActive,nextSsActive-1);
							lastActive = subMap2.lastKey() - truesplength;
							if (firstActive == lastActive) {
								lastActive++;
							}
							else if (firstActive > lastActive) {
								skipPath = true;
								continue;
							}
							else {
								SortedMap<Integer,Note> subMap3 = noteMap.subMap(firstActive,lastActive);
								if (subMap3.size() > 0) {
									lastActive = subMap3.lastKey();
								}
							}
						}
						else if (sp > 0){
							int last = noteMap.lastKey();
							Note lastNote = noteMap.get(last);
							lastActive = lastNote.getTime() - truesplength;
							if (j == s.length() - 1 && nextStarMapSection!= 0) {
								lastActive = nextStarMapSection - truesplength;
							}
							if (firstActive == lastActive) {
								lastActive++;
							}
							else if (firstActive > lastActive) {
								skipPath = true;
								continue;
							}
							else
							 {
								SortedMap<Integer,Note> subMap4 = noteMap.subMap(lastActive,noteMap.lastKey());
								if (!subMap4.isEmpty())
									lastActive = subMap4.firstKey();
							 }
						}*/

							firstActive = findFirstAct(spValues);

							if (firstActive == -1) {
								skipPath = true;
								continue;
							}

							lastActive = findLastAct(spValues);

							if (firstActive == lastActive) {
								lastActive++;
							}
							else if (firstActive > lastActive) {
								skipPath = true;
								continue;
							}

							Activation a = getHighestScore(firstActive,lastActive,truesplength,maxsplength,spValues);

							if (a.takeFromNext) {
								truesplength = (int) (truesplength + a.getSpFromNext());
								double spFromCheck = a.getSpFromNext();
								Activation a2 = getHighestScore(a.begin,lastActive,truesplength,maxsplength,spValues);
								while (spFromCheck < a2.getSpFromNext()) {
									spFromCheck = a2.getSpFromNext() - 1;
									a2 = getHighestScore(a2.begin,lastActive,truesplength,maxsplength,spValues);
									if (a2.getSpFromNext() - 1 == spFromCheck) {
										spFromCheck++;
									}
								}
								a = a2;
							}

							if (activations.size()==0) {
								continue;
							}
							int lastStar = activations.get(activations.size()-1);
							String activeNumber = "";
							if (a.begin < lastStar) {
								for (int m = 0; m < activations.size(); m++) {
									if (a.begin < activations.get(m)) {
										activeNumber = m + "(" + (currentSp - m) + ")";
										m = activations.size();
									}
								}
							}
							else {
								activeNumber = "" + currentSp;
							}
							a.setActiveNumber(activeNumber);
							int acti = Integer.parseInt(activeNumber.substring(0,1));
							double actiMeasures = 0.0;
							for (int act = 0; act < acti; act++) {
								actiMeasures = actiMeasures + spValues.get(act).getMeasures() + spValues.get(act).returnLength();
							}
							actiMeasures = Math.round(actiMeasures / 8 * 100);
							if (actiMeasures > 100) {
								actiMeasures = 100;
							}
							//double mesDis = (1.0 * splength) /( 1.0 * resolution)/tsMes;
							String activeDetail = activeNumber + " (" + df.format(actiMeasures) + "% SP)\n";
							//pathDetail.append(bestActivation+"\n");

							Note activeNote = noteMap.get(a.begin);
							int afterStar = Integer.parseInt(Character.toString(activeNumber.charAt(0)));

							boolean onNote = true;
							int activeBefore = 0;
							if (activeNote == null) {
								SortedMap<Integer,Note> subMap5 = noteMap.subMap(a.begin,noteMap.lastKey());
								activeNote = subMap5.get(subMap5.firstKey());
								onNote = false;
								activeBefore = activeNote.getTime() - a.begin;
							}

							if (afterStar != 0) {
								int mapStart = activations.get(afterStar - 1);
								SortedMap<Integer,Note> subMap6 = noteMap.subMap(mapStart+1,activeNote.getTime()+1);
								String noteFret = activeNote.getFret();
								activeDetail = activeDetail + fretCounter(subMap6,noteFret,activeBefore,onNote) + 
										" (Beat " + df.format(1.0 * a.begin/resolution) + ")\n";

								//pathDetail.append(activeDetail+"\n");

								if (squeeze && a.end > 0) {
									SortedMap<Integer,Note> subMap7 = noteMap.subMap(activeNote.getTime()+1,a.end);
									if (subMap7.size() > 0) {
										String noteFret2 = subMap7.get(subMap7.lastKey()).getFret();
										activeDetail = activeDetail + "Squeeze " +fretCounter(subMap7,noteFret2,0,onNote) +
												" (Beat " + df.format(1.0 * subMap7.lastKey()/resolution) + ")\n";
										//pathDetail.append("Squeeze " + activeDetail +"\n");
									}
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
								a.setActiveDetail(activeDetail);
								pathDetail.append(activeDetail);
								if (actualPath.length() != 0 || smk > 0) {
									actualPath = actualPath + ", ";
								}
								actualPath = actualPath + activeNumber;
								comboScore = comboScore + a.getScore();

								setTakeNext(a.takeFromNext);
								setNextSp(a.spFromNext);

								acts.put(key,a);
							}
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
						//bestPathLiteral = s;
						bestPathDetail = pathDetail.toString();
					}
				}

				totalBestComboScore = totalBestComboScore + bestComboScore;
				totalBestPath = totalBestPath + bestPath;
				totalBestPathDetail = totalBestPathDetail + bestPathDetail;
			}
			pathScore = pathScore + totalBestComboScore + soloBonus;
			output.append("Best Path: " + totalBestPath + "\n");
			output.append("Score = " + pathScore + "\n\n");
			output.append(totalBestPathDetail);
			return pathScore;
		}
		catch (Exception e) {

			e.printStackTrace();
			return 0;
		}

	}

	public String fretCounter (SortedMap<Integer,Note> subMap, String noteFret, int activeBefore, boolean onNote) {

		if (subMap.size() <= 1) {
			return "NN";
		}

		int fretCounter = 0;

		for (Map.Entry<Integer, Note> entry : subMap.entrySet()) {
			Note nn = entry.getValue();
			if (nn.getFret().equals(noteFret)) {
				fretCounter++;
			}
		}
		if (fretCounter == 0) {
			fretCounter = 1;
		}

		String colorFret = "";
		if (!sixFret) {
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
		}
		else {
			for (int p = 0; p < noteFret.length(); p++ ) {
				Character color = noteFret.charAt(p);
				switch (color) {
				case '0':
					colorFret = colorFret + "1W";
					break;
				case '1':
					colorFret = colorFret + "2W";
					break;
				case '2':
					colorFret = colorFret + "3W";
					break;
				case '3':
					colorFret = colorFret + "1B";
					break;
				case '4':
					colorFret = colorFret + "2B";
					break;
				case '7':
					colorFret = "Open";
					break;
				case '8':
					colorFret = colorFret + "3B";
					break;
				}
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
			activeDetail = df.format(beatsBefore) + " Beats Before ";
		}
		activeDetail = activeDetail + fc + " " + noteFret;

		return activeDetail;

	}

	public int findFirstAct (ArrayList<StarSection> sss) {
		int firstAct = 0;
		int firstActSS = 0;
		double measures = 0.0;
		boolean locTakeNext = takeFromNext;

		for (int i = 0; i < sss.size(); i++) {
			StarSection ss = sss.get(i);
			double lv = ss.returnLength();
			if (locTakeNext) {
				lv = lv - (spFromNext/3.75);
				locTakeNext = false;
			}
			measures = measures + ss.getMeasures() + lv;
			double lastmeasures = 0.0;

			if (measures >= 4 && firstAct == 0) {
				int act = getActivationNote(ss.getTime(),ss.getLength());
				firstAct = act + 1;
				firstActSS = i + 1;
			}

			else if (i < sss.size() - 1 && firstAct == 0) {
				StarSection nss = sss.get(i+1);
				double checkLength = nss.returnLength() + measures;
				if (checkLength >= 4) {
					checkLength = checkLength - nss.returnLength();
					SortedMap<Integer,Note> subMap = noteMap.subMap(nss.getTime(),nss.getTime()+nss.getLength());
					for (Map.Entry<Integer, Note> entry : subMap.entrySet())  {
						Note n = entry.getValue();
						if (n.getLength() > 0) {
							int len = n.getLength();
							if (earlyWhammy) {
								len = (int) (len + posEarly);
							}
							double lv2 = (double) len / resolution;
							lv2 = lv2 / 3.75;
							checkLength = checkLength + lv2;
							if (checkLength >= 4) {
								double checkLength2 = checkLength - 4;
								if (checkLength2 < 0) {
									checkLength2 = 0;
								}
								firstAct = n.getTime() + n.getLength() - ((int) (checkLength2 * 3.75 * resolution));
								firstActSS = i + 1;
								lastmeasures = measures;
								measures = 4;
							}
						}
					}
				}
			}

			if (firstAct != 0) {

				updateSync(firstAct);

				double tsMes = ts.getTop() / ts.getBottom() * 4;
				int splength = calcSpLength(firstAct,measures,sss.size(),firstActSS);
				/*if (squeeze) {
					splength = (int) (splength + posEarly + posLate);
				}*/
				//int end = firstAct + splength;

				//int check = checkExtraSPNoMod(firstAct,end,measures);

				if (splength == -1) {
					firstAct = 0;
					firstActSS = 0;
					if (lastmeasures != 0)
						measures = lastmeasures;
				}

				else if (splength == 0) {
					return -1;
				}
				else {
					return firstAct;
				}
			}

		}

		return firstAct;

	}

	public int findLastAct (ArrayList<StarSection> sss) {
		int lastAct = 0;
		double measures = 0.0;
		int beginSearch = 0;

		for (int i = 0; i < sss.size(); i++) {
			StarSection ss = sss.get(i);
			measures = measures + ss.getMeasures() + ss.returnLength();
			if (measures > 8) {
				measures = 8;
			}
		}

		beginSearch = getActivationNote(sss.get(sss.size()-1).getTime(),sss.get(sss.size()-1).getLength());

		int lastEnd = 0;
		if (starMap.lastKey() == sss.get(sss.size()-1).getTime()) {
			lastEnd = noteMap.lastKey();
		}
		else {
			SortedMap<Integer,StarSection> subMap = starMap.subMap(beginSearch,starMap.lastKey());
			StarSection ss = null;
			if (subMap.size() > 0) {
				ss = subMap.get(subMap.firstKey());
			}
			else {
				ss = sss.get(sss.size()-1);
			}
			if (!lastSyncEvent) {
				lastSyncIndex = updateSync(ss.getTime(),lastSyncIndex);
			}
			int extraLen = 0;
			if (ss.returnLength() > 0) {
				double tsMes = ts.getTop() / ts.getBottom() * 4;
				extraLen = (int) Math.ceil(tsMes * ss.returnLength() * resolution);
			}
			lastEnd = getActivationNote(ss.time,ss.length) - extraLen;
		}

		double tsMes = ts.getTop() / ts.getBottom() * 4;
		int splength = (int) Math.ceil(tsMes * measures * resolution);
		if (squeeze) {
			splength = (int) (splength + posEarly + posLate);
		}

		lastAct = lastEnd - splength - 1;

		int truesplength = calcSpLengthNoError(lastAct,measures,sss.size(),sss.size());
		if (truesplength <= 0) {
			truesplength = splength;
		}

		lastAct = lastEnd - truesplength - 1;

		return lastAct;
	}

	public int getHighestScore (int firstAct, int lastAct, ArrayList<StarSection> sss) {
		int activationPoint = 0;
		int highestScore = 0;
		int bestSp = 0;
		int splength = 0;
		boolean actualTakeNext = false;
		double actualNextSp = 0.0;
		double tempNextSp = 0.0;
		boolean tempTakeNext = false;

		if (firstAct == 0 || firstAct == -1.0) {
			return 0;
		}

		for (int i = firstAct; i <= lastAct; i++) {

			double sp = 0;

			/*if (i == 0) {
				System.out.println("Test");
			}*/

			for (int j = 0; j < sss.size(); j++) {
				StarSection jss = sss.get(j);
				int act = getActivationNote(jss.getTime(),jss.getLength());
				if (act < i) {
					sp = sp + jss.getMeasures() + jss.returnLength();
					if (j == 0 && takeFromNext) {
						double lv = spFromNext/3.75;
						sp = sp - lv;
					}
					if (sp > 8.0) {
						sp = 8.0;
					}
					if (!lastSyncEvent) {
						lastSyncIndex = updateSync(i,lastSyncIndex);
					}

					double tsMes = ts.getTop() / ts.getBottom() * 4;
					splength = (int) Math.ceil(tsMes * sp * resolution);

				}
				else {
					double penalty = spLeft(i, act, sp);
					double lastv = 0;
					if (jss.returnLength() > 0) {
						penalty = penalty + jss.returnLength();
						int lastLength = noteMap.get(act).getLength();
						if (lastLength > 0) {
							if (earlyWhammy) {
								lastLength = lastLength + (int) posEarly;
							}
							lastv = (double) lastLength / resolution;
							lastv = lastv / 3.75;
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

						lastv = lastv * whammy;
					}

					penalty = penalty - lastv;

					if (penalty > 6) {
						penalty = penalty - 6;
					}
					else {
						penalty = 0;
					}

					double sp2 = jss.getMeasures() + jss.returnLength() - penalty;					

					sp = sp + sp2;

					if (!lastSyncEvent) {
						lastSyncIndex = updateSync(i,lastSyncIndex);
					}

					double tsMes = ts.getTop() / ts.getBottom() * 4;
					int splength2 = (int) Math.ceil(tsMes * sp2 * resolution);

					splength = splength + splength2;
				}
			}

			int sqLen = 0;
			if (squeeze) {
				sqLen = (int) (posEarly + posLate);
			}
			splength = splength + sqLen;

			int act = getActivationNote(sss.get(sss.size()-1).getTime(),sss.get(sss.size()-1).getLength());
			if (act < i+splength) {
				SortedMap<Integer,StarSection> subMap = starMap.subMap(act,i+splength);
				if (subMap.size() >= 1) {
					StarSection ss = subMap.get(subMap.firstKey());
					if (ss.returnLength() > 0) {
						double nspsum = 0;
						int sLen = getLengthSum(ss.getTime(),i+splength);
						while (sLen > 0) {
							int end = i+splength;
							double nsp;
							nsp = (double) sLen / resolution;
							nsp = nsp / 3.75;
							nspsum = nspsum + nsp;
							tempNextSp = nspsum;
							tempTakeNext = true;
							splength = splength + sLen;
							sLen = getLengthSum(end,i+splength);
						}
					}
				}
				else {
					tempNextSp = 0;
					tempTakeNext = false;
				}
			}
			else {
				tempNextSp = 0;
				tempTakeNext = false;
			}

			int sectionScore = getValueSum(i,i+splength);
			if (sectionScore > highestScore) {
				actualTakeNext = tempTakeNext;
				actualNextSp = tempNextSp;
				highestScore = sectionScore;
				activationPoint = i;
				bestSp = splength;
			}
			else if (sectionScore == highestScore) {
				Note note = noteMap.get(i);
				Note note2 = noteMap.get(activationPoint);
				if (note2 == null && note != null) {
					activationPoint = i;
					actualTakeNext = tempTakeNext;
					actualNextSp = tempNextSp;
					bestSp = splength;
				}
			}
		}

		setTakeNext(actualTakeNext);
		setNextSp(actualNextSp);
		lastBestScore = bestScore;
		bestScore = highestScore;
		setBestSpLength(bestSp);
		return activationPoint;
	}


	public Activation getHighestScore (int firstAct, int lastAct, int sp, int maxsp, ArrayList<StarSection> sss) {
		int activationPoint = 0;
		int highestScore = 0;
		double highestSp = 0;
		boolean takeNext = false;
		double next = 0.0;
		//setTakeNext(false);
		//setNextSp(0);

		for (int i = firstAct; i <= lastAct; i++) {
			double maxcheck = 0;
			double nextcheck = 0;
			Note nnn = null;
			int currentsections = 0;
			//int fullSp = 0;
			for (int j = 0; j < sss.size(); j++) {
				StarSection jss = sss.get(j);
				SortedMap<Integer,Note> subMap2 = noteMap.subMap(jss.getTime(),jss.getTime()+jss.getLength());
				Note jnn = subMap2.get(subMap2.lastKey());
				if (jnn.getTime() <= i) {
					maxcheck = maxcheck + jss.getMeasures() + jss.returnLength();
					if (maxcheck > 8.0) {
						maxcheck = 8.0;
					}
					currentsections++;
				}
				/*else {
					nextcheck = jss.returnLength();
					if (nextcheck == 0) {
						nextcheck = nextcheck + .01;
					}
					nnn = jnn;
					j = sss.size();
				}*/
			}
			/*double totalcheck = maxcheck;
			if (nextcheck > 0) {
				if (nextcheck == .01) {
					nextcheck = nextcheck - .01;
				}
				totalcheck = totalcheck + 2.0 + nextcheck;
			}*/
			/*updateSync(i);
			double tsMes = ts.getTop() / ts.getBottom() * 4.0;*/

			int currentSp =  calcSpLength(i,maxcheck,sss.size(),currentsections);

			if (currentSp == -1) {
				continue;
			}

			if (currentSp == 0) {
				i = lastAct + 1;
			}

			/*if (nextcheck > 0) {
				if (nextcheck == .01) {
					nextcheck = nextcheck - .01;
				}
				SortedMap<Integer,SyncEvent> subMapts = tsMap.subMap(i, nnn.getTime());
				if (subMapts.size() == 0) {
					double spcheck = spLeft (i,nnn.getTime(),maxcheck+nextcheck);
					if (spcheck > 6.0) {
						spcheck = spcheck - 6.0;
						int splength = (int) Math.round(tsMes * spcheck * resolution);
						fullSp = currentSp - splength;
					}
					else if ( nextcheck < 0) {
						continue;
					}
					else {
						fullSp = currentSp;
					}
				}
				else {

				}
			}*/
			//else {
			//fullSp = currentSp;
			//}

			int max = currentSp + i;

			//updateSync(max);
			/*if (squeeze) {
				max = max + (int)posLate;
			}*/

			StarSection last =  sss.get(sss.size()-1);

			int lastact = getActivationNote(last.getTime(), last.getLength());
			if (lastact > max) {
				continue;
			}

			int sqLen = 0;
			if (squeeze) {
				sqLen = (int) (posEarly + posLate);
			}
			//splength = splength + sqLen;
			updateSync(last.getTime()+last.getLength());
			double tsMes = ts.getTop() / ts.getBottom() * 4;
			int maxsplength = (int) Math.ceil(tsMes * 8 * resolution);
			maxsplength = maxsplength + sqLen;

			/*if (i > max)
				System.out.println("");*/

			int sectionScore = getValueSum(i,max);
			if (sectionScore > highestScore) {
				StarSection lastss = sss.get(sss.size()-1);
				int lastactive = getActivationNote(lastss.getTime(), lastss.getLength());
				/*if (lastactive > max) {
					System.out.println("");
				}*/
				SortedMap<Integer,StarSection> subMap = starMap.subMap(lastactive,max);
				if (subMap.size() >= 1) {
					StarSection ss = subMap.get(subMap.firstKey());
					if (ss.returnLength() > 0) {
						int sLen = getLengthSum(ss.getTime(),max);
						if (sLen > 0) {
							takeNext = true;
							double nsp;
							nsp = (double) sLen / resolution;
							nsp = nsp / 3.75;
							next = nsp;
						}
					}
				}
				highestScore = sectionScore;
				highestSp = max;
				activationPoint = i;
			}
			else if (sectionScore == highestScore) {
				Note note = noteMap.get(i);
				Note note2 = noteMap.get(activationPoint);
				if (note2 == null && note != null) {
					StarSection lastss = sss.get(sss.size()-1);
					int lastactive = getActivationNote(lastss.getTime(), lastss.getLength());
					SortedMap<Integer,StarSection> subMap = starMap.subMap(lastactive,max);
					if (subMap.size() >= 1) {
						StarSection ss = subMap.get(subMap.firstKey());
						if (ss.returnLength() > 0) {
							int sLen = getLengthSum(ss.getTime(),max);
							if (sLen > 0) {
								takeNext = true;
								double nsp;
								nsp = (double) sLen / resolution;
								nsp = nsp / 3.75;
								next = nsp;
							}
						}
					}
					activationPoint = i;
					highestSp = max;

				}
			}
		}

		//double tsMes = ts.getTop() / ts.getBottom() * 4;
		//int bestSp = (int) Math.ceil(highestSp);

		//lastBestScore = bestScore;

		Activation a = new Activation(activationPoint,((int)highestSp),highestScore);
		a.setTakeFromNext(takeNext);
		a.setSpFromNext(next);
		bestScore = highestScore;
		setBestSpLength((int)highestSp);
		return a;

	}

	public double spLeft (int active, int current, double sp) {
		double newsp = 0.0;

		int spsplit = current - active;

		if (spsplit < 0) {
			return 0;
		}

		SortedMap<Integer,SyncEvent> subMapts = tsMap.subMap(active, current);
		if (subMapts.size() > 0) {
			double currentsp = sp;
			int curr = active;
			updateSync(active);
			double tsMes = ts.getTop() / ts.getBottom() * 4;
			for (Map.Entry<Integer, SyncEvent> entry : subMapts.entrySet()) {
				SyncEvent e = entry.getValue();
				int endcurrent = e.getInd();
				int spspl = endcurrent - curr;
				double meselapsed = spspl / resolution / tsMes;
				currentsp = currentsp - meselapsed;
				curr = endcurrent;
				updateSync(curr);
				tsMes = ts.getTop() / ts.getBottom() * 4;
			}
			spsplit = current - curr;

			double meselapsed = spsplit / resolution / tsMes;
			newsp = currentsp - meselapsed;

			return newsp;
		}
		else {
			updateSync(current);
			double tsMes = ts.getTop() / ts.getBottom() * 4;
			double meselapsed = spsplit / resolution / tsMes;
			newsp = sp - meselapsed;

			return newsp;
		}
	}

	public double returnSpLength (int l, int t) {
		//Need to figure out way to adjust if bpm or ts changes in middle of hold note
		double lv = 0.0;

		if (l > 0) {
			updateSync(t);

			if (earlyWhammy) {
				l = (int) (l + posEarly);
			}
			lv = (double) l / resolution;
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


	public int calcSpLength (int active, double sp, int sectionsize, int currentsections) {
		int splength = 0;
		double currentsp = sp;

		updateSync(active);
		double tsMes = ts.getTop() / ts.getBottom() * 4;

		splength = (int) Math.ceil(tsMes * currentsp * resolution);
		if (squeeze)
			splength = splength + (int)posEarly;

		SortedMap<Integer,SyncEvent> subMapts = tsMap.subMap(active, active+splength);
		if (subMapts.size() > 0) {
			int newsplength = 0;
			if (squeeze)
				newsplength = newsplength + (int)posEarly;
			int current = active;
			for (Map.Entry<Integer, SyncEvent> entry : subMapts.entrySet()) {
				SyncEvent e = entry.getValue();
				int endcurrent = e.getInd();
				double spcheck = spLeft(current,endcurrent,currentsp);
				if (spcheck <= 0) {
					continue;
				}
				newsplength = newsplength + (int) Math.ceil(tsMes * (currentsp - spcheck) * resolution);
				currentsp = spcheck;
				current = endcurrent;
				updateSync(current);
				tsMes = ts.getTop() / ts.getBottom() * 4;
			}
			newsplength = newsplength + (int) Math.ceil(tsMes * (currentsp) * resolution);
			splength = newsplength;	

			/*if (splength <= 0) {
				System.out.println("");
			}*/
		}

		SortedMap<Integer,StarSection> subMapSs = starMap.subMap(active, active+splength);
		int sssize = subMapSs.size();
		int orsplength = splength;
		double orsp = currentsp;
		if (sssize + currentsections > sectionsize) {
			return 0;
		}
		while (sssize > 0) {
			int mapcounter = 0;
			for (Map.Entry<Integer, StarSection> entry : subMapSs.entrySet()) {
				StarSection ss = entry.getValue();
				double extra = ss.returnLength();

				SortedMap<Integer,Note> subMapno = noteMap.subMap(ss.getTime(), ss.getTime()+ss.getLength());
				Note not = subMapno.get(subMapno.lastKey());
				if (not.getLength() > 0) {
					extra = extra - returnSpLength(not.getLength(), not.getTime());
				}

				double spcheck = spLeft(active,ss.getTime(),currentsp+extra);
				if (spcheck <= 0) {
					return -1;
				}
				double penalty = 0;
				if (spcheck > 6.0) {
					double over = spcheck - 6.0;
					if (over > 2.0) {
						over = 2.0;
					}
					//extra = extra - over;
					penalty = over;
				}
				currentsp = currentsp + 2.0 + ss.returnLength() - penalty;
				splength = (int) Math.ceil(tsMes * currentsp * resolution);
				if (squeeze)
					splength = splength + (int)posEarly;
				mapcounter++;
				/*if (splength <= 0) {
						System.out.println("");
					}*/	
			}
			subMapSs = starMap.subMap(active, active+splength);
			if (subMapSs.size() + currentsections > sectionsize) {
				return 0;
			}
			sssize = subMapSs.size() - mapcounter;
			if (sssize > 0) {
				splength = orsplength;
				currentsp = orsp;
			}
		}
		updateSync(active+splength);
		if (squeeze)
			splength = splength + (int)posLate;

		/*if (splength <= 0) {
			System.out.println("");
		}*/	

		return splength;
	}

	public int calcSpLengthNoError (int active, double sp, int sectionsize, int currentsections) {
		int splength = 0;
		double currentsp = sp;

		updateSync(active);
		double tsMes = ts.getTop() / ts.getBottom() * 4;

		splength = (int) Math.ceil(tsMes * currentsp * resolution);
		if (squeeze)
			splength = splength + (int)posEarly;

		SortedMap<Integer,SyncEvent> subMapts = tsMap.subMap(active, active+splength);
		if (subMapts.size() > 0) {
			int newsplength = 0;
			if (squeeze)
				newsplength = newsplength + (int)posEarly;
			int current = active;
			for (Map.Entry<Integer, SyncEvent> entry : subMapts.entrySet()) {
				SyncEvent e = entry.getValue();
				int endcurrent = e.getInd();
				double spcheck = spLeft(current,endcurrent,currentsp);
				if (spcheck <= 0) {
					continue;
				}
				newsplength = newsplength + (int) Math.ceil(tsMes * (currentsp - spcheck) * resolution);
				currentsp = spcheck;
				current = endcurrent;
				updateSync(current);
				tsMes = ts.getTop() / ts.getBottom() * 4;
			}
			newsplength = newsplength + (int) Math.ceil(tsMes * (currentsp) * resolution);
			splength = newsplength;	

			/*if (splength <= 0) {
				System.out.println("");
			}*/
		}

		SortedMap<Integer,StarSection> subMapSs = starMap.subMap(active, active+splength);
		int sssize = subMapSs.size();
		int orsplength = splength;
		double orsp = currentsp;
		/*if (sssize + currentsections > sectionsize) {
				return 0;
			}*/
		while (sssize > 0) {
			int mapcounter = 0;
			for (Map.Entry<Integer, StarSection> entry : subMapSs.entrySet()) {
				StarSection ss = entry.getValue();
				double extra = ss.returnLength();

				SortedMap<Integer,Note> subMapno = noteMap.subMap(ss.getTime(), ss.getTime()+ss.getLength());
				Note not = subMapno.get(subMapno.lastKey());
				if (not.getLength() > 0) {
					extra = extra - returnSpLength(not.getLength(), not.getTime());
				}

				double spcheck = spLeft(active,ss.getTime(),currentsp+extra);
				/*if (spcheck <= 0) {
						return -1;
					}*/
				double penalty = 0;
				if (spcheck > 6.0) {
					double over = spcheck - 6.0;
					if (over > 2.0) {
						over = 2.0;
					}
					//extra = extra - over;
					penalty = over;
				}
				currentsp = currentsp + 2.0 + ss.returnLength() - penalty;
				splength = (int) Math.ceil(tsMes * currentsp * resolution);
				if (squeeze)
					splength = splength + (int)posEarly;
				mapcounter++;
				/*if (splength <= 0) {
						System.out.println("");
					}*/	
			}
			subMapSs = starMap.subMap(active, active+splength);
			/*if (subMapSs.size() + currentsections > sectionsize) {
					return 0;
				}*/
			sssize = subMapSs.size() - mapcounter;
			if (sssize > 0) {
				splength = orsplength;
				currentsp = orsp;
			}
		}
		updateSync(active+splength);
		if (squeeze)
			splength = splength + (int)posLate;

		/*if (splength <= 0) {
			System.out.println("");
		}*/	

		return splength;
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

	public void checkTensCombos (ArrayList<ArrayList<Integer>> CList, int n) {

		int arr[] = new int [n];
		ArrayList<int[]> comboList = new ArrayList<int[]>();

		findCombinationsUtilOverTen(arr,comboList,0,n,n);

		for (int i = 0; i < comboList.size(); i++) {
			ArrayList<int[]> allCombos = new ArrayList<int[]>();
			ArrayList<String> goodCombos = new ArrayList<String>();
			int[] str = new int[comboList.get(i).length];
			int digit = 0;
			boolean sameDigits = true;
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
			boolean skip = false;
			for (int j = 0; j < str.length; j++) {
				str[j] = comboList.get(i)[j];
				if (str[j] < 10 || str[j] >= 16) {
					skip = true;
					j = str.length;
				}
				if (digit == 0) {
					digit = str[j];
				}
				if (j > 0) {
					if (str[j] != digit) {
						sameDigits = false;
					}
				}
			}

			if (skip) {
				continue;
			}

			if (str.length == 1) {
				goodCombos.add(str[0] + "");
			}
			else if (str.length == 2) {
				String s1 = str[0] + "";
				String s2 = str[1] + "";
				goodCombos.add(s1+s2);
				goodCombos.add(s2+s1);
			}
			else if (sameDigits) {
				String s = "";
				for (int sd =0; sd < str.length; sd++) {
					s = s + str[sd];
				}
				goodCombos.add(s);
			}
			else {
				QuickPerm(str,goodCombos);
			}

			for (int l = 0; l < goodCombos.size(); l++) {
				String s = goodCombos.get(l);

				Character c = null;
				if (s.length() % 2 != 0) {
					c = s.charAt(s.length()-1);
					s = s.substring(0, s.length()-2);
				}

				CList.add(new ArrayList<Integer>());

				for (int sl = 0; sl < s.length(); sl = sl +2) {
					String sc = "" + s.charAt(sl) + s.charAt(sl + 1);
					CList.get(CList.size()-1).add(Integer.parseInt(sc));
				}
			}
		}
	}

	public void checkStarCombos (ArrayList<String> CList, SortedMap<Integer,StarSection> starsub) {

		ArrayList<Integer> ones = CheckOnes(starsub);
		numberOfOnes = ones.size();
		int n = starsub.size();
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
					if (n > 15 && t >= 6) {
						add = false;
						jn = temp.length;
					}
					if (n > 30 && t >= 5) {
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

		ArrayList<String> goodCombos = new ArrayList<String>();

		for (int i = 0; i < comboList.size(); i++) {
			ArrayList<String> allCombos = new ArrayList<String>();
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
				if (digit == 0) {
					digit = str[j];
				}
				if (j > 0) {
					if (str[j] != digit) {
						sameDigits = false;
						if (digit == 1) {
							digit = str[j];
						}
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
					int total = 0;
					for (int oner = 0; oner < str.length; oner++) {
						total = total + str[oner];
						if (str[oner] == 1) {
							if (!ones.contains(total)){
								addThis = false;
								continue; 
							}
							else
								sb.setCharAt(oner, '1');
						}
						else {
							sb.setCharAt(oner, (char) str[oner]);
						}
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
					int total = 0;
					for (int oner = 0; oner < ones.size(); oner++) {
						total = total + str[oner];
						if (str[oner] == 1) {
							if (!ones.contains(total)){
								addThis = false;
								continue; 
							}
							else
								sb.setCharAt(oner, '1');
						}
						else {
							sb.setCharAt(oner, (char) str[oner]);
						}
					}
					if (!addThis) {
						continue;
					}
					s = sb.toString();
					if (!allCombos.contains(s)) {
						allCombos.add(s);
					}
				}
				else {
					String s = "";
					for (int st = 0; st < str.length; st++) {
						s = s + str[st];
					}
					FastPerm(s,"",allCombos);
				}
			}
			else {
				String s = "";
				for (int st = 0; st < str.length; st++) {
					s = s + str[st];
				}
				FastPerm(s,"",allCombos);
			}

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
						for (int m = 0; m < s.length(); m++) {
							int thisSpNum = Character.getNumericValue(s.charAt(m));
							spNum = spNum + thisSpNum;
							if (thisSpNum == 1 && !ones.contains(spNum)) {
								add = false;
							}
						}
					}
				}

				if (add) {
					goodCombos.add(s);
				}	

				/*if (add) {
					ArrayList<StarSection> ssections = new ArrayList<StarSection>();

					for (Map.Entry<Integer, StarSection> entry : starsub.entrySet()) {
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

							updateSync(ss.getTime());

							double lv = ss.returnLength();

							sp = sp + ss.getMeasures() + lv;
							if (sp > 8) {
								sp = 8;
							}

							active = getActivationNote(ss.getTime(), ss.getLength());
							SortedMap<Integer,Note> subMap = noteMap.subMap(active,noteMap.lastKey());
							active = subMap.firstKey();
						}

						if (sp < 4) {
							sp = 0;
						}

						updateSync(active);

						double tsMes = ts.getTop() / ts.getBottom() * 4;
						int splength = (int) Math.ceil(tsMes * sp * resolution);
						int end = active + splength;

						int check = checkExtraSPNoMod(active,end,sp);

						if (check != 0 && totalSp < ssections.size()) {
							System.out.println("Overlap");
							add = false;
						}
					}*/

			}

		}

		//System.out.println("Total combos: " + allCombos.size());

		for (int a = 0; a < goodCombos.size(); a++) {
			CList.add(goodCombos.get(a));
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
					if (n > 15 && t >= 6) {
						add = false;
						jn = temp.length;
					}
					if (n > 30 && t >= 5) {
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

		ArrayList<String> goodCombos = new ArrayList<String>();

		for (int i = 0; i < comboList.size(); i++) {
			ArrayList<String> allCombos = new ArrayList<String>();
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
				if (digit == 0) {
					digit = str[j];
				}
				if (j > 0) {
					if (str[j] != digit) {
						sameDigits = false;
						if (digit == 1) {
							digit = str[j];
						}
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
					int total = 0;
					for (int oner = 0; oner < str.length; oner++) {
						total = total + str[oner];
						if (str[oner] == 1) {
							if (!ones.contains(total)){
								addThis = false;
								continue; 
							}
							else
								sb.setCharAt(oner, '1');
						}
						else {
							sb.setCharAt(oner, (char) str[oner]);
						}
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
					int total = 0;
					for (int oner = 0; oner < ones.size(); oner++) {
						total = total + str[oner];
						if (str[oner] == 1) {
							if (!ones.contains(total)){
								addThis = false;
								continue; 
							}
							else
								sb.setCharAt(oner, '1');
						}
						else {
							sb.setCharAt(oner, (char) str[oner]);
						}
					}
					if (!addThis) {
						continue;
					}
					s = sb.toString();
					if (!allCombos.contains(s)) {
						allCombos.add(s);
					}
				}
				else {
					String s = "";
					for (int st = 0; st < str.length; st++) {
						s = s + str[st];
					}
					FastPerm(s,"",allCombos);
				}
			}
			else {
				String s = "";
				for (int st = 0; st < str.length; st++) {
					s = s + str[st];
				}
				FastPerm(s,"",allCombos);
			}

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
						for (int m = 0; m < s.length(); m++) {
							int thisSpNum = Character.getNumericValue(s.charAt(m));
							spNum = spNum + thisSpNum;
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

						if (sp < 4) {
							sp = 0;
						}

						if (!lastSyncEvent) {
							lastSyncIndex = updateSync(active,lastSyncIndex);
						}

						double tsMes = ts.getTop() / ts.getBottom() * 4;
						int splength = (int) Math.ceil(tsMes * sp * resolution);
						int end = active + splength;

						if (!lastSyncEvent) {
							end = checkSync(active,end,sp);
						}

						int check = checkExtraSPNoMod(active,end,sp);

						if (check != 0 && totalSp < ssections.size()) {
							//System.out.println("Overlap");
							add = false;
						}
					}
					if (add) {
						goodCombos.add(s);
					}	
				}


			}
		}

		//System.out.println("Total combos: " + allCombos.size());

		for (int a = 0; a < goodCombos.size(); a++) {
			CList.add(goodCombos.get(a));
		}		
	}

	public void findCombinationsUtilOverTen(int arr[], ArrayList<int[]> arr2, int index, 
			int num, int reducedNum) 
	{ 		
		if (reducedNum < 0) 
			return; 

		if (reducedNum == 0) 
		{ 
			int tempArr[] = new int[index];

			for (int i = 0; i < index; i++) {
				if (arr[i]!=0) {
					tempArr[i] = arr[i];
					if (arr[i] < 10 || arr[i] >= 16) {
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
			findCombinationsUtilOverTen(arr, arr2, index + 1, num, 
					reducedNum - k); 
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
		int splength = (int) Math.ceil(tsMes * sp * resolution);

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
				int spl = (int) Math.ceil(tsMes * newsp * resolution);
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
								int splength2 = (int) Math.ceil(tsMes * newsp * resolution);
								end = nlList.get(i).getTime() + splength2;
								end = checkSync(nlList.get(i).getTime(),end,newsp);
								spused = spused + lv2;
							}
						}

						newsp = newsp + 2;
						if (newsp > 8) {
							newsp = 8;
						}
						int splength2 = (int) Math.ceil(tsMes * newsp * resolution);
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
						int splength2 = (int) Math.ceil(tsMes * newsp * resolution);
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
								int splength2 = (int) Math.ceil(tsMes * newsp * resolution);
								end = nlList.get(i).getTime() + splength2;
								end = checkSync(nlList.get(i).getTime(),end,newsp);
								//spused = spused + lv2;
							}
						}

						newsp = newsp + 2;
						if (newsp > 8) {
							newsp = 8;
						}
						int splength2 = (int) Math.ceil(tsMes * newsp * resolution);
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
						int splength2 = (int) Math.ceil(tsMes * newsp * resolution);
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

	public ArrayList<Integer> CheckOnes (SortedMap<Integer,StarSection> starsub) {
		ArrayList<Integer> ones = new ArrayList<Integer>();
		int i = 1;

		for (Map.Entry<Integer, StarSection> entry : starsub.entrySet())  {
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

		double top = 4.0;
		double bottom = 4.0;

		public TimeSig () {}

		public TimeSig (int t, int b) {
			this.top = t * 1.0;
			this.bottom = t * 1.0;
		}

		public double getTop() {
			return top;
		}

		public void setTop(int top) {
			this.top = top;
		}

		public double getBottom() {
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
				//updateSync(time);

				for (Map.Entry<Integer, Note> entry : nlMap.entrySet())  {
					Note n = entry.getValue();
					if (n.length>0) {
						nn++;
					}
				}
				double nnn = nn * posEarly * 1.5;
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

	public class Activation {
		int begin;
		int end;
		int score = 0;
		String activeNumber = "";
		String activeDetail = "";
		boolean takeFromNext = false;
		double spFromNext = 0.0;

		public Activation (int b, int e, int s) {
			this.begin = b;
			this.end = e;
			this.score = s;
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

		public int getScore() {
			return score;
		}

		public void setScore(int score) {
			this.score = score;
		}

		public String getActiveNumber() {
			return activeNumber;
		}

		public void setActiveNumber(String activeNumber) {
			this.activeNumber = activeNumber;
		}

		public String getActiveDetail() {
			return activeDetail;
		}

		public void setActiveDetail(String activeDetail) {
			this.activeDetail = activeDetail;
		}

		public void appendActiveDetail (String app) {
			this.activeDetail = this.activeDetail + app;
		}

		public boolean isTakeFromNext() {
			return takeFromNext;
		}

		public void setTakeFromNext(boolean takeFromNext) {
			this.takeFromNext = takeFromNext;
		}

		public double getSpFromNext() {
			return spFromNext;
		}

		public void setSpFromNext(double spFromNext) {
			this.spFromNext = spFromNext;
		}

	}

	public static void main(String[] args) {
		StarPather test = new StarPather();

		File chart = new File("C:/Users/tmerwitz/Downloads/notes (9).chart");
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
		//test.setInstrument("EasySingle");
		test.parseFile(is);
		test.printStarMap();
		//ArrayList<String> combos = new ArrayList<String>();
		//test.checkStarCombos(combos);
		//test.ultraEasyFullPath();
		//test.noSqueezePath();
		//System.out.println(test.getOutput());
		System.out.println(test.bestPathEver());
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		//System.out.println("Ultra Easy Score: " + test.ultraEasyPath());
		//System.out.println("Ultra Easy NW Score: " + test.ultraEasyNoWhammy());
		//System.out.println("Ultra Easy Full SP Score: " + test.ultraEasyFullPath());
	}	

}
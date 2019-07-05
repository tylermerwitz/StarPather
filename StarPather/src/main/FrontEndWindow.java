package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import main.StarPather.Note;
import swt.SWTResourceManager;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Combo;

public class FrontEndWindow {

	protected Shell shlStar;
	private Text txtTest;
	private Text txtNoSu;
	private Button btnSelectchart;
	public String chart = "";
	private Button buttonRun;
	public StarPather path;
	private Display display;
	private String instrument = "";
	private String difficulty = "";
	private HashMap<String,ArrayList<String>> map = new HashMap<String,ArrayList<String>>();

	String whammyType = "";
	String squeezeType = "";
	String diaBox = "";
	private ProgressBar progressBar;
	private Label lblDifficulty;
	private Combo whammy_combo;
	private Label lblSqueeze;
	private Combo squeeze_combo;
	private Label lblSqueeze_1;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			FrontEndWindow window = new FrontEndWindow();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		display = Display.getDefault();
		createContents();
		shlStar.open();
		shlStar.layout();		
		
		while (!shlStar.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlStar = new Shell();
		shlStar.setSize(400, 513);
		shlStar.setText("StarPather");

		txtTest = new Text(shlStar, SWT.BORDER | SWT.READ_ONLY | SWT.CENTER);
		txtTest.setText(chart);
		txtTest.setBounds(86, 61, 288, 21);
		
		Combo diff_combo = new Combo(shlStar, SWT.DROP_DOWN | SWT.READ_ONLY);
		diff_combo.setItems(new String[] {"Expert"});
		//diff_combo.setItem(0, "Expert");
		diff_combo.setBounds(267, 88, 98, 23);
		diff_combo.select(0);
		
		diff_combo.addSelectionListener(new SelectionAdapter() {
			 
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = diff_combo.getSelectionIndex();
                difficulty = diff_combo.getItem(idx);
            }
        });
		
		lblDifficulty = new Label(shlStar, SWT.NONE);
		lblDifficulty.setText("Difficulty");
		lblDifficulty.setFont(org.eclipse.wb.swt.SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		lblDifficulty.setBounds(197, 88, 62, 23);
		
		Combo inst_combo = new Combo(shlStar, SWT.DROP_DOWN | SWT.READ_ONLY);
		inst_combo.setItems(new String[] {"Lead Guitar"});
		inst_combo.setBounds(86, 88, 105, 20);
		inst_combo.select(0);
		
		inst_combo.addSelectionListener(new SelectionAdapter() {
			 
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = inst_combo.getSelectionIndex();
                instrument = inst_combo.getItem(idx);
                
                String[] ray2 = new String[map.get(instrument).size()];
				diff_combo.setItems(new String[] {""});
				
				 for (int i = 0; i < map.get(instrument).size(); i++) {
	                	ray2[i] = map.get(instrument).get(i);
	                }
				 
				diff_combo.setItems(ray2);
				diff_combo.select(0);
				difficulty = ray2[0];
            }
        });
		
		Label lblNewLabel = new Label(shlStar, SWT.NONE);
		lblNewLabel.setFont(org.eclipse.wb.swt.SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		lblNewLabel.setBounds(5, 88, 75, 23);
		lblNewLabel.setText("Instrument");
		
		whammy_combo = new Combo(shlStar, SWT.DROP_DOWN | SWT.READ_ONLY);
		whammy_combo.setBounds(86, 117, 105, 23);
		whammy_combo.setItems(new String[] {"Early (+65 ms)", "Full (100%)", "Lazy (80%)", "Bad (50%)", "Zero (0%)"});
		whammy_combo.select(0);
		
		whammy_combo.addSelectionListener(new SelectionAdapter() {
			 
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = whammy_combo.getSelectionIndex();
                whammyType = whammy_combo.getItem(idx);
            }
        });
		
		lblSqueeze = new Label(shlStar, SWT.NONE);
		lblSqueeze.setText("Whammy");
		lblSqueeze.setFont(org.eclipse.wb.swt.SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		lblSqueeze.setBounds(13, 117, 68, 23);
		
		squeeze_combo = new Combo(shlStar, SWT.DROP_DOWN | SWT.READ_ONLY);
		squeeze_combo.setItems(new String[] {"Yes", "No"});
		squeeze_combo.setBounds(267, 117, 98, 23);
		squeeze_combo.select(0);
		
		squeeze_combo.addSelectionListener(new SelectionAdapter() {
			 
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = squeeze_combo.getSelectionIndex();
                squeezeType = squeeze_combo.getItem(idx);
            }
        });
		
		lblSqueeze_1 = new Label(shlStar, SWT.NONE);
		lblSqueeze_1.setText("Squeeze");
		lblSqueeze_1.setFont(org.eclipse.wb.swt.SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		lblSqueeze_1.setBounds(203, 117, 58, 23);

		btnSelectchart = new Button(shlStar, SWT.NONE);
		btnSelectchart.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				map.clear();
				FileDialog fd = new FileDialog(shlStar, SWT.OPEN);
				fd.setText("Open");
				fd.setFilterPath("C:/");
				String[] filterExt = { "*.chart"};
				fd.setFilterExtensions(filterExt);
				chart = fd.open();
				txtTest.setText(chart);

				if (chart!="") {
					File f = new File(chart);
					if (!f.exists()) {
						diaBox = "File selected could not be found. Please re-select chart file.";
						txtNoSu.setText(diaBox);
					}
					else {
						btnSelectchart.setEnabled(true);
						InputStream is = null;
						try {
							is = new FileInputStream(chart);
						} catch (FileNotFoundException er) {
							// TODO Auto-generated catch block
							er.printStackTrace();
						}

						path = new StarPather();

						path.getInstruments(is, map);
						
						String[] ray = new String[map.size()];
						inst_combo.setItems(new String[] {""});
						int mapI = 0;

						for (Map.Entry<String, ArrayList<String>> entry : map.entrySet())  {
							ray[mapI] = entry.getKey();
							mapI++;
						}
						
						inst_combo.setItems(ray);
						inst_combo.select(ray.length - 1);
						
						String def = "Lead Guitar";
						
						while (!map.containsKey(def)) {
							switch (def) {
							case "Lead Guitar":
								def = "Co-Op Guitar";
								break;
							case "Co-Op Guitar":
								def = "Bass";
								break;
							case "Bass":
								def = "Rhythm Guitar";
								break;
							case "Rhythm Guitar":
								def = "Keyboard";
								break;
							case "Keyboard":
								def = "6 Fret Guitar";
								break;
							case "6 Fret Guitar":
								def = "6 Fret Bass";
								break;
							}
						}
						
						String[] ray2 = new String[map.get(def).size()];
						diff_combo.setItems(new String[] {""});
						
						 for (int i = 0; i < map.get(def).size(); i++) {
			                	ray2[i] = map.get(def).get(i);
			                }
						 
						diff_combo.setItems(ray2);
						diff_combo.select(0);
						instrument = def;
						difficulty = ray2[0];
					}
				}
			}


			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnSelectchart.setBounds(5, 59, 75, 25);
		btnSelectchart.setText("Select .chart");

		txtNoSu = new Text(shlStar, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);

		StringBuffer usage = new StringBuffer();
		usage.append("Welcome to StarPather V1.3.0!\n");
		usage.append("Created by Tyler Merwitz AKA MathyNoodles\n");
		usage.append("Last Updated: 07/05/19\n\n");
		usage.append("This program was designed to optimize the Star Power path\n");
		usage.append("for any .chart file. Please report any bugs to this program's\n");
		usage.append("github page:\n");
		usage.append("https://github.com/tylermerwitz/StarPather\n\n");
		usage.append("Special thanks to Nemo296 for his invaluable info on\n");
		usage.append("the .chart format and helping me refine a ton of the\n");
		usage.append("scoring logic!\n\n");
		usage.append("Useage:\n");
		usage.append("First, click the \"Select .chart\" button near the top of\n");
		usage.append(" the window and find the .chart file you wish to use\n");
		usage.append("After your chart is selected, you may toggle these options:\n");
		usage.append("     -Instrument: Select the instrument the program will\n");
		usage.append("      return a path for.\n");
		usage.append("     -Difficulty: Select the difficulty the program will\n");
		usage.append("      return a path for.\n");
		usage.append("     -Whammy: Select the average amount of extra star\n");
		usage.append("      power you will receive for every star power\n");
		usage.append("      sustain note in the chart\n");
		usage.append("     -Squeeze: Toggle whether or not you will be squeezing\n");
		usage.append("       in Star Power phrases\n");
		usage.append("Finally, hit the \"Optimize\" button, and viola!\n");
		usage.append("The program will print the Star Power path requested!\n\n");
		usage.append("Note: It is recommend you use this tool in combination\n");
		usage.append("with Gutair_Hero_Tools.exe in order to get a better\n");
		usage.append("understanding and visualization of where to activate\n");
		usage.append("https://www.youtube.com/watch?v=LMY3HyK09js\n");

		txtNoSu.setText(usage.toString());
		txtNoSu.setEditable(false);
		txtNoSu.setBounds(10, 207, 364, 263);

		Label lblTestGui = new Label(shlStar, SWT.NONE);
		lblTestGui.setFont(SWTResourceManager.getFont("Segoe UI", 22, SWT.NORMAL));
		lblTestGui.setBounds(135, 10, 130, 43);
		lblTestGui.setText("StarPather");

		buttonRun = new Button(shlStar, SWT.NONE);
		buttonRun.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {

				if (chart!="") {
					File f = new File(chart);
					if (!f.exists()) {
						diaBox = "File selected could not be found. Please re-select chart file.";
						txtNoSu.setText(diaBox);
					}
					else {
						InputStream is = null;
						try {
							is = new FileInputStream(chart);
						} catch (FileNotFoundException er) {
							// TODO Auto-generated catch block
							er.printStackTrace();
						}
						
						path = new StarPather();

						/*if (pathType.equals("FullSqueeze")) {
							try {
								path.setSqueeze(true);
								path.setEarlyWhammy(true);
								path.parseFile(is);
								//diaBox = path.bestPathEver();
							}
							catch (Exception er) {
								String ert = er.toString();
								txtNoSu.setText(ert);
							}
						}
						else if (pathType.equals("Easy")) {
							try {
								path.parseFile(is);
								//diaBox = path.bestPathEver();
							}
							catch (Exception er) {
								String ert = er.toString();
								txtNoSu.setText(ert);
							}
						}
						else if (pathType.equals("Ultra")) {
							try {
								path.setLazyWhammy(true);
								path.parseFile(is);
								//diaBox = path.bestPathEver();
							}
							catch (Exception er) {
								String ert = er.toString();
								txtNoSu.setText(ert);
							}
						}
						else if (pathType.equals("Early")) {
							try {
								path.setEarlyWhammy(true);
								path.parseFile(is);
								//diaBox = path.bestPathEver();
							}
							catch (Exception er) {
								String ert = er.toString();
								txtNoSu.setText(ert);
							}
						}*/
						
						switch (whammyType) {
						case "Early (+65 ms)":
							path.setEarlyWhammy(true);
							break;
						case "Full (100%)":
							break;
						case "Lazy (80%)":
							path.setLazyWhammy(true);
							break;
						case "Bad (50%)":
							path.setBadWhammy(true);
							break;
						case "Zero (0%)":
							path.setNoWhammy(true);
							break;
						}
						
						switch (squeezeType) {
						case "Yes":
							path.setSqueeze(true);
							break;
						case "No":
							break;
						}
						
						String comboDif = difficulty;
						
						switch (instrument) {
						case "Lead Guitar":
							comboDif = comboDif + "Single";
							break;
						case "Co-Op Guitar":
							comboDif = comboDif + "DoubleGuitar";
							break;
						case "Bass":
							comboDif = comboDif + "DoubleBass";
							break;
						case "Rhythm Guitar":
							comboDif = comboDif + "DoubleRhythm";
							break;
						case "Keyboard":
							comboDif = comboDif + "Keyboard";
							break;
						case "6 Fret Guitar":
							comboDif = comboDif + "GHLGuitar";
							break;
						case "6 Fret Bass":
							comboDif = comboDif + "GHLBass";
							break;
						}
						
						path.setInstrument(comboDif);
						
						try {
							path.parseFile(is);
						}
						catch (Exception er) {
							String ert = er.toString();
							txtNoSu.setText(ert);
						}

						//diaBox = path.getOutput();
						new Thread() {
						      public void run() {
						          if (display.isDisposed()) {
						              return;
						          }
						          this.updateGUIWhenStart();
						          
						  		String orDetail = path.getOutput();
						  		
						  		if (path.starMap.size() < 23 || path.starMap.size() > 70) {
						  			path.noSqueezePath();
						  			diaBox = path.getOutput();
						  		}
						  		else {
						  			int n = path.starMap.size();
						  			ArrayList<ArrayList<Integer>> comboList = new ArrayList<ArrayList<Integer>>();

						  			path.checkTensCombos(comboList,n);
						  			
						  			int bestScore = 0;
						  			String bestDetail = "";
						  			
						  			for (int i = 0; i < comboList.size(); i++) {
						  				
						  				this.updateGUIInProgress(i, comboList.size());
						  				this.copy();
						  				
						  				int score = path.noSqueezePathAlt(comboList.get(i));
						  				String currentDetail = path.getOutput();
						  				
						  				if (score > bestScore) {
						  					bestScore = score;
						  					bestDetail = currentDetail;
						  				}
						  				
						  			}
						  			
						  			String detail = orDetail + bestDetail;
						  			
						  			diaBox = detail;
						  		}
						          //
						          this.updateGUIWhenFinish();
						      }
						      private void copy() {
							        try {
							            Thread.sleep(100);
							        } catch (InterruptedException e) {
							        }
							    }
						  
						    private void updateGUIWhenStart() {
						        display.asyncExec(new Runnable() {
						 
						            @Override
						            public void run() {
						                buttonRun.setEnabled(false);
						                buttonRun.setText("Running...");
						                btnSelectchart.setEnabled(false);
						            }
						        });
						    }
						 
						    private void updateGUIWhenFinish() {
						        display.asyncExec(new Runnable() {
						 
						            @Override
						            public void run() {
						            	buttonRun.setEnabled(true);
						            	btnSelectchart.setEnabled(false);
						                progressBar.setSelection(0);
						                progressBar.setMaximum(1);
						                txtNoSu.setText(diaBox);
						                buttonRun.setText("Optimize!");
						            }
						        });
						    }
						 
						    private void updateGUIInProgress(int value, int count) {
						        display.asyncExec(new Runnable() {
						 
						            @Override
						            public void run() {
						            	progressBar.setMaximum(count);
						                progressBar.setSelection(value);						             
						            }
						        });
						    }
						    }.start();

					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		buttonRun.setBounds(135, 146, 124, 32);
		buttonRun.setText("Optimize!");
		
		progressBar = new ProgressBar(shlStar, SWT.NONE);
		progressBar.setBounds(86, 184, 240, 17);

	}
	public Button getBtnSelectchart() {
		return btnSelectchart;
	}
	public Text getText() {
		return txtTest;
	}
}

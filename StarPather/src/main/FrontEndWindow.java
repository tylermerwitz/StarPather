package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

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
import swt.SWTResourceManager;

public class FrontEndWindow {

	protected Shell shlStar;
	private Text txtTest;
	private Button btnEasy;
	private Button btnEarlyWhammy;
	private Button btnUltra;
	private Text txtNoSu;
	private Button btnSelectchart;
	public String chart = "";
	private Button btnNewButton;

	String pathType = "";
	String diaBox = "";

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
		Display display = Display.getDefault();
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
		shlStar.setSize(391, 463);
		shlStar.setText("StarPather");

		txtTest = new Text(shlStar, SWT.BORDER | SWT.READ_ONLY | SWT.CENTER);
		txtTest.setText(chart);
		txtTest.setBounds(91, 61, 274, 21);

		btnSelectchart = new Button(shlStar, SWT.NONE);
		btnSelectchart.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(shlStar, SWT.OPEN);
				fd.setText("Open");
				fd.setFilterPath("C:/");
				String[] filterExt = { "*.chart"};
				fd.setFilterExtensions(filterExt);
				chart = fd.open();
				txtTest.setText(chart);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnSelectchart.setBounds(10, 59, 75, 25);
		btnSelectchart.setText("Select .chart");

		Button btnFullSqueeze = new Button(shlStar, SWT.RADIO);
		btnFullSqueeze.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "FullSqueeze";
			}
		});
		btnFullSqueeze.setBounds(101, 88, 90, 16);
		btnFullSqueeze.setText("Full Squeeze");

		btnEasy = new Button(shlStar, SWT.RADIO);
		btnEasy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "Easy";
			}
		});
		btnEasy.setText("Easy");
		btnEasy.setBounds(101, 110, 90, 16);

		btnEarlyWhammy = new Button(shlStar, SWT.RADIO);
		btnEarlyWhammy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "Early";
			}
		});
		btnEarlyWhammy.setText("Early Whammy");
		btnEarlyWhammy.setBounds(195, 88, 100, 16);

		btnUltra = new Button(shlStar, SWT.RADIO);
		btnUltra.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "Ultra";
			}
		});
		btnUltra.setText("Ultra Easy");
		btnUltra.setBounds(195, 110, 90, 16);

		txtNoSu = new Text(shlStar, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);

		StringBuffer usage = new StringBuffer();
		usage.append("Welcome to StarPather V1.0.5!\n");
		usage.append("Created by Tyler Merwitz AKA MathyNoodles\n");
		usage.append("Last Updated: 06/06/2019\n\n");
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
		usage.append("Next, select one of the four options for pathing:\n");
		usage.append("     -Full Squeeze: Find the optimal star power path for the\n");
		usage.append("      chart entered with squeezing and early whammying.\n");
		usage.append("     -Early Whammy: Find the optimal star power path for the\n");
		usage.append("      chart entered with NO squeezing and early whammying\n");
		usage.append("      whammying.\n");
		usage.append("     -Easy: Find the optimal star power path for the chart\n");
		usage.append("      entered with NO squeezing and NO early whammying\n");
		usage.append("     -Ultra Easy: Returns the score you will get on this\n");
		usage.append("      chart if you always activate star power as soon as\n");
		usage.append("      it's available\n");
		usage.append("Finally, hit the \"Optimize\" button, and viola!\n");
		usage.append("The program will print the Star Power path requested!\n\n");
		usage.append("Note: It is recommend you use this tool in combination\n");
		usage.append("with Gutair_Hero_Tools.exe in order to get a better\n");
		usage.append("understanding and visualization of where to activate\n");
		usage.append("https://www.youtube.com/watch?v=LMY3HyK09js\n");

		txtNoSu.setText(usage.toString());
		txtNoSu.setEditable(false);
		txtNoSu.setBounds(10, 172, 355, 242);

		Label lblTestGui = new Label(shlStar, SWT.NONE);
		lblTestGui.setFont(SWTResourceManager.getFont("Segoe UI", 22, SWT.NORMAL));
		lblTestGui.setBounds(123, 10, 129, 43);
		lblTestGui.setText("StarPather");

		btnNewButton = new Button(shlStar, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionListener() {
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

						StarPather path = new StarPather();

						if (pathType.equals("FullSqueeze")) {
							try {
								path.setSqueeze(true);
								path.setEarlyWhammy(true);
								path.parseFile(is);
								path.noSqueezePath();
							}
							catch (Exception er) {
								String ert = er.toString();
								txtNoSu.setText(ert);
							}
						}
						else if (pathType.equals("Easy")) {
							try {
								path.parseFile(is);
								path.noSqueezePath();
							}
							catch (Exception er) {
								String ert = er.toString();
								txtNoSu.setText(ert);
							}
						}
						else if (pathType.equals("Ultra")) {
							try {
								path.setBadWhammy(true);
								path.parseFile(is);
								path.ultraEasyPath();
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
								path.noSqueezePath();
							}
							catch (Exception er) {
								String ert = er.toString();
								txtNoSu.setText(ert);
							}
						}

						diaBox = path.getOutput();
						txtNoSu.setText(diaBox);

					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		btnNewButton.setBounds(125, 134, 105, 32);
		btnNewButton.setText("Optimize!");

	}
	public Button getBtnSelectchart() {
		return btnSelectchart;
	}
	public Text getText() {
		return txtTest;
	}
}

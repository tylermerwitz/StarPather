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
	private Button btnLazy;
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
		
		Button btnNoSqueeze = new Button(shlStar, SWT.RADIO);
		btnNoSqueeze.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "NoSqueeze";
			}
		});
		btnNoSqueeze.setBounds(101, 88, 90, 16);
		btnNoSqueeze.setText("No Squeeze");
		
		btnEasy = new Button(shlStar, SWT.RADIO);
		btnEasy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "Easy";
			}
		});
		btnEasy.setText("Easy");
		btnEasy.setBounds(101, 110, 90, 16);
		
		btnLazy = new Button(shlStar, SWT.RADIO);
		btnLazy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "Lazy";
			}
		});
		btnLazy.setText("Lazy Whammy");
		btnLazy.setBounds(195, 88, 100, 16);
		
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
		usage.append("Welcome to StarPather V1.0.4!\n");
		usage.append("Created by Tyler Merwitz\n");
		usage.append("Last Updated: 05/30/2019\n\n");
		usage.append("This program was designed to optimize the Star Power path\n");
		usage.append("for any .chart file. Please report any bugs to this\n");
		usage.append("program's github page:\n");
		usage.append("https://github.com/tylermerwitz/StarPather\n");
		usage.append("Special thanks to Nemo296 for his invaluable info on\n");
		usage.append("the .chart format and helping me refine a ton of the\n");
		usage.append("scoring logic!\n");
		usage.append("Useage:\n");
		usage.append("First, click the \"Select .chart\" button near the top of\n");
		usage.append(" the window and find the .chart file you wish to use\n");
		usage.append("Next, select one of the four options for pathing:\n");
		usage.append("     -No Squeeze: Find the optimal star power path for the\n");
		usage.append("      chart entered if you do NOT squeeze any notes.\n");
		usage.append("     -Lazy Whammy: Finds the optimal no squeeze path when\n");
		usage.append("      you get ~80% of the maximum star power from\n");
		usage.append("      whammying.\n");
		usage.append("     -Ultra Easy: Returns the score you will get on this\n");
		usage.append("      chart if you always activate star power as soon as\n");
		usage.append("      it is available and do not use the whammy.\n");
		usage.append("     -Easy: Returns the score you will get on this chart\n");
		usage.append("      if you always activate star power as soon as it's\n");
		usage.append("      available\n");
		usage.append("Finally, hit the \"Optimize\" button, and viola!\n");
		usage.append("The program will print the Star Power path requested!\n");
		usage.append("Note: It is recommend you use this tool in combination\n");
		usage.append("with Gutair_Hero_Tools.exe in order to get a better\n");
		usage.append("understanding and visualization of where to activate\n");
		usage.append("Star Power\n");
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
						
						if (pathType.equals("NoSqueeze")) {
							path.parseFile(is);
							path.noSqueezePath();
						}
						else if (pathType.equals("Easy")) {
							path.parseFile(is);
							path.ultraEasyPath();
						}
						else if (pathType.equals("Ultra")) {
							path.setNoWhammy(true);
							path.parseFile(is);
							path.ultraEasyPath();
						}
						else if (pathType.equals("Lazy")) {
							path.setLazyWhammy(true);
							path.parseFile(is);
							path.noSqueezePath();
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

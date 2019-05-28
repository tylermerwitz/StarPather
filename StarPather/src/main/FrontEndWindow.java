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
import org.eclipse.wb.swt.SWTResourceManager;

public class FrontEndWindow {

	protected Shell shlStar;
	private Text txtTest;
	private Button btnEasy;
	private Button btnUltraEasy;
	private Button btnEasyFull;
	private Text text_1;
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
		txtTest.setBounds(91, 88, 274, 21);
		
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
		btnSelectchart.setBounds(10, 86, 75, 25);
		btnSelectchart.setText("Select .chart");
		
		Button btnNoSqueeze = new Button(shlStar, SWT.RADIO);
		btnNoSqueeze.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "NoSqueeze";
			}
		});
		btnNoSqueeze.setBounds(111, 123, 90, 16);
		btnNoSqueeze.setText("No Squeeze");
		
		btnEasy = new Button(shlStar, SWT.RADIO);
		btnEasy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "Easy";
			}
		});
		btnEasy.setText("Easy");
		btnEasy.setBounds(111, 145, 90, 16);
		
		btnUltraEasy = new Button(shlStar, SWT.RADIO);
		btnUltraEasy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "Ultra";
			}
		});
		btnUltraEasy.setText("Ultra Easy");
		btnUltraEasy.setBounds(205, 123, 90, 16);
		
		btnEasyFull = new Button(shlStar, SWT.RADIO);
		btnEasyFull.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathType = "Full";
			}
		});
		btnEasyFull.setText("Easy full");
		btnEasyFull.setBounds(205, 145, 90, 16);
		
		text_1 = new Text(shlStar, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		text_1.setEditable(false);
		text_1.setBounds(10, 207, 355, 207);
		
		Label lblTestGui = new Label(shlStar, SWT.NONE);
		lblTestGui.setFont(SWTResourceManager.getFont("Segoe UI", 22, SWT.NORMAL));
		lblTestGui.setBounds(111, 21, 129, 43);
		lblTestGui.setText("StarPather");
		
		btnNewButton = new Button(shlStar, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {

				if (chart!="") {
					File f = new File(chart);
					if (!f.exists()) {
						diaBox = "File selected could not be found. Please re-select chart file.";
						text_1.setText(diaBox);
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
						path.parseFile(is);
						
						if (pathType.equals("NoSqueeze")) {
							path.noSqueezePath();
						}
						else if (pathType.equals("Easy")) {
							path.ultraEasyPath();
						}
						else if (pathType.equals("Ultra")) {
							path.ultraEasyNoWhammy();
						}
						else if (pathType.equals("EasyFull")) {
							path.ultraEasyFullPath();
						}
						
						diaBox = path.getOutput();
						text_1.setText(diaBox);
						
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		btnNewButton.setBounds(135, 169, 105, 32);
		btnNewButton.setText("Optimize!");

	}
	public Button getBtnSelectchart() {
		return btnSelectchart;
	}
	public Text getText() {
		return txtTest;
	}
}

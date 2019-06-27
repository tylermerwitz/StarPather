package main;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
 
public class RunThread extends Thread {
 
    private Display display;
    private ProgressBar progressBar;
    private Button buttonRun;
    private Text txtNoSu;
    private boolean squeeze = false;
    private boolean early = false;
    private boolean lazy = false;
    private StarPather sp = null;
    public String diabox;
    
    public FrontEndWindow few;
 
    public RunThread(FrontEndWindow few) {
        this.few = few;
    }
 
    public boolean isSqueeze() {
		return squeeze;
	}

	public void setSqueeze(boolean squeeze) {
		this.squeeze = squeeze;
	}

	public boolean isEarly() {
		return early;
	}

	public void setEarly(boolean early) {
		this.early = early;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	@Override
    public void run() {
        if (display.isDisposed()) {
            return;
        }
        this.updateGUIWhenStart();
        
		String orDetail = sp.getOutput();
		
		if (sp.starMap.size() < 23 || sp.starMap.size() > 70) {
			sp.noSqueezePath();
			diabox = sp.getOutput();
		}
		else {
			int n = sp.starMap.size();
			ArrayList<ArrayList<Integer>> comboList = new ArrayList<ArrayList<Integer>>();

			sp.checkTensCombos(comboList,n);
			
			int bestScore = 0;
			String bestDetail = "";
			
			for (int i = 0; i < comboList.size(); i++) {
				int score = sp.noSqueezePathAlt(comboList.get(i));
				String currentDetail = sp.getOutput();
				
				if (score > bestScore) {
					bestScore = score;
					bestDetail = currentDetail;
				}
				
				this.updateGUIInProgress(i, comboList.size());
				this.copy();
			}
			
			String detail = orDetail + bestDetail;
			
			diabox = detail;
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
            }
        });
    }
 
    private void updateGUIWhenFinish() {
        display.asyncExec(new Runnable() {
 
            @Override
            public void run() {
            	buttonRun.setEnabled(true);
                progressBar.setSelection(0);
                progressBar.setMaximum(1);
                txtNoSu.setText(diabox);
            }
        });
    }
 
    private void updateGUIInProgress(int value, int count) {
        display.asyncExec(new Runnable() {
 
            @Override
            public void run() {
                progressBar.setSelection(value);
            }
        });
    }
 
}

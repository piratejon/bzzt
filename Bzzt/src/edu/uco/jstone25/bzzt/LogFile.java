package edu.uco.jstone25.bzzt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;

public class LogFile {
	
	PrintWriter pw;

	public void appendFile(String s) throws IOException {
		pw.print(SystemClock.elapsedRealtimeNanos() + ":" + s + "\n");
	}
	
	public void openOutputFile(File directory, String prefix) throws IOException {
		File f = new File(directory, prefix + System.currentTimeMillis() + ".txt");
		pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f))));
		appendFile("start stamp (ns)");
	}
	
	public LogFile(String prefix) throws IOException {
		openOutputFile(Environment.getExternalStoragePublicDirectory(Context.DOWNLOAD_SERVICE), prefix);
	}
	
	public void close() {
		pw.flush();
		pw.close();
	}
}

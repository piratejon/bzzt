package edu.uco.jstone25.bzzt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

public class LogFile {
	
	GZIPOutputStream zos;
	File f;

	public void appendFile(String s) throws IOException {
		String s2 = SystemClock.elapsedRealtimeNanos() + "," + s + "\n";
		zos.write(s2.getBytes());
	}
	
	public void openOutputFile(File directory, String prefix) throws IOException {
		f = new File(directory, prefix + System.currentTimeMillis() + ".txt.gz");
		zos = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
		appendFile("start stamp (ns)");
	}
	
	public LogFile(String prefix) throws IOException {
		openOutputFile(Environment.getExternalStoragePublicDirectory(Context.DOWNLOAD_SERVICE), prefix);
	}
	
	public void close() {
		try {
			zos.flush();
			zos.close();
		} catch (IOException e) {
			Log.d("BzztLogFileException", e.toString());
			e.printStackTrace();
		}
	}
	
	public String getCanonicalPath() {
		try {
			return f.getCanonicalPath();
		} catch (IOException e) {
			return f.getAbsolutePath();
		}
	}
}

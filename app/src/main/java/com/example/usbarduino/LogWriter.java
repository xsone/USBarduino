/*
 * Copyright (C) 2014 Petrolr LLC, a Colorado limited liability company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* 
 * Written by the Petrolr team in 2014. Based on the Android SDK Bluetooth Chat Example... matthew.helm@gmail.com
 */


package com.example.usbarduino;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.AccessController;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import android.widget.Toast;

public class LogWriter {


	protected static void write_info(final String logmsg) {

		// ++++ Fire off a thread to write info to file
		
		Thread w_thread = new Thread() {
			public void run() {
				//File myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android");
				//myFilesDir.mkdirs();
				File myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/usblog");
				myFilesDir.mkdirs();
					    
				//String dataline = (timestamp() + "; " + logmsg + "\n");
				String dataline = (datestamp() + ";" + timestamp() + ";" + logmsg + "\n");
				File myfile = new File(myFilesDir + "/" + "usblog" + sDate() + ".csv");
				if(myfile.exists() == true){
					try {
						FileWriter write = new FileWriter(myfile, true);
						write.append(dataline);
						//read_ct++;
						write.close();
					}catch (IOException e){
						
					}
				}else{ //make a new file since we apparently need one
					try {
						FileWriter write = new FileWriter(myfile, true);
						//	write.append(header);
						write.append(dataline);
						//read_ct++;
						write.close();		
					}catch (IOException e){
						    		
					}
	
				}
			}
		};
		w_thread.start();
	}

	/*
	protected static String timestamp(){
		long timestamp = System.currentTimeMillis();
		return String.valueOf(timestamp);
	}
	*/

	protected static String datestamp(){
		Calendar calen = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String sDateLog = sdf.format(calen.getTime());
		return sDateLog;
	}

	protected static String timestamp(){
		Calendar calen = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String sDateLog = sdf.format(calen.getTime());
		return sDateLog;
	}


	@SuppressLint("SimpleDateFormat")
	protected static String sDate(){
		//Calendar calen = Calendar.getInstance();
		//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//String sDate = sdf.format(calen.getTime());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		java.util.Date date= new java.util.Date();
		String sDate = sdf.format(date.getTime());
		return sDate;
	}
}

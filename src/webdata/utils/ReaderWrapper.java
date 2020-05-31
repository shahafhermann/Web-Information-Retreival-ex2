package webdata.utils;

import java.io.*;


public class ReaderWrapper implements Comparable<ReaderWrapper> {

	BufferedReader br;
	webdata.utils.Line currLine;

	public ReaderWrapper(BufferedReader br, webdata.utils.Line currLine){
		this.br = br;
		this.currLine = currLine;
	}

	public Line getCurrLine(){
		return currLine;
	}

	public boolean advancePtr() throws IOException{
		String line = br.readLine();
		if (line != null){
			currLine = new Line(line);
			return true;
		}
		currLine = null;
		br.close();
		return false;
	}

	@Override
	public int compareTo(ReaderWrapper o) {
		return currLine.compareTo(o.currLine);
	}
}

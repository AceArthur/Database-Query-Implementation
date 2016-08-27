package source;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
 
public class tfile {
	private String filename;
	private String filepath;
	private String[] attributes;
	private List<int[]> data;

	public tfile(String filepath) {
		this.filepath = filepath;
		init();
	}

	private void init() {
		try {
			File file = new File(filepath);
			filename = file.getName();
			if (!file.exists()) {
				file = new File(filepath);
				if (!file.exists()) {
					System.out.println("Error: " + filepath + " does not exist.");
					System.exit(1);
				}
			}
			BufferedReader reader = new BufferedReader(new FileReader(filepath));
			String columnnames = reader.readLine();// Attribute names
			attributes = columnnames.split(",");
			data = new ArrayList<int[]>();
			String line = null;
			while ((line = reader.readLine()) != null) {
				String tuple_str[] = line.split(",");
				int tuple[] = new int[tuple_str.length];
				for (int i = 0; i < tuple_str.length; i++) {
					tuple[i] = Integer.parseInt(tuple_str[i]);
				}
				data.add(tuple);
			}
			reader.close();
		} catch (Exception e) {
			System.out.println("Error initializating " + filename + ".");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public int numAttributes() {
		return attributes.length;
	}

	public int numTuples() {
		return data.size();
	}

	public String[] getAttributes() {
		return attributes;
	}

	public int[] getTuple(int index) {
		return data.get(index);
	}

	public String getFilename() {
		return filename;
	}
}

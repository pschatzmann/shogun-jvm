package org.shogun;

import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.commons.io.IOUtils;

/**
 * Loads the native binary libraries which are a precondition to use shogun.jar.
 * 
 * @author pschatzmann
 *
 */

public class ShogunNative {
	public static final String VERSION = "6.1.3";
	public static final String NAME = "shogun";

	/**
	 * Loads all necessary libraries @throws
	 */
	public static void load() {
		String error = "";
		try {
			NativeLibraryLoader nat = new NativeLibraryLoader(NAME, VERSION);
			nat.load();
			shogun.init_shogun_with_defaults();
		} catch (Throwable ex) {
			try {
				InputStream is = NativeLibraryLoader.class.getResourceAsStream("/error.txt");
				error = IOUtils.toString(is, "UTF8");
			} catch (Exception ex1) {
				ex1.printStackTrace();
			}
			String msg = ex.getMessage() + System.lineSeparator() + error;
			throw new RuntimeException(msg);
		}
	}

	public static double[][] toArray2(java.util.List<java.util.List<?>> list) {
		return toArray2(list, 0, 0, 0, 0);
	}

	/**
	 * Converts a list of lists to a 2 dimensional array
	 * @param list
	 * @return
	 */
	public static double[][] toArray2(java.util.List<java.util.List<?>> list, int startRow, int rows, int startCol, int cols ){
		double[][] result = new double[rows][cols];
		int rowID = 0;
		for (java.util.List<?> row  : list.subList(startRow, startRow+rows)) {
			int colID = 0;
			for (Object v : row.subList(startCol, startCol+cols) ) {
				if (v!=null) {
					result[rowID][colID] = getDoubleValue(v);
				}
				colID++;
			}
			rowID++;
		}
		return result;
	}
	
	
	
	/**
	 * Converts a list of lists to a 1 dimensional array
	 * @param list
	 * @return
	 */
	public static double[] toArray(java.util.List<java.util.List<?>> list, int startRow, int rows, int col ){
		double[] result = new double[rows];
		int rowID = 0;
		for (java.util.List<?> row  : list.subList(startRow, startRow+rows)) {
			Object v = row.get(col);
			if (v!=null) {
				result[rowID] = getDoubleValue(v);
			}	
			rowID++;
		}
		return result;
	}

	/**
	 * Converts an object to a Double
	 * @param obj
	 * @return
	 */
	public static double getDoubleValue(Object obj) {		
		
		if (obj==null) {
			return 0;
		}
		
		if (obj instanceof Number) {
			Number n = (Number) obj;
			Double result = n.doubleValue();
			return result;
		}

		try {
			Method toDouble = obj.getClass().getDeclaredMethod("toDouble");
			if (toDouble != null) {
				Double result = (Double) toDouble.invoke(obj, null);
				return result;
			}
		} catch (Exception ex) {
		}

		double result = Double.valueOf(obj.toString());
		return result;
	}

}

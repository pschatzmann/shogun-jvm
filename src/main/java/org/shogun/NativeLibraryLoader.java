package org.shogun;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java Class to manage native libraries. We load the main jar which is defined
 * as root node in the lddtree.txt file.
 * 
 * Optionally we can install the folder with all binaries in the temp directory
 * and load the binaries from there.
 * 
 * We support complex library scenarios by interpreting the dependency hierarchy
 * which is provided by lddtree.
 * 
 * @author pschatzmann
 *
 */
public class NativeLibraryLoader {
	private static final Logger LOG = LoggerFactory.getLogger(NativeLibraryLoader.class);
	private String name;
	private String version;
	private boolean forceCopy = false;
	private String platform = null;
	private DependencyTreeNode dependenciesRoot;
	private Throwable lastException;
	private boolean supportsInstall;

	/**
	 * Defines the library. The name is used to identify the prefix of the resource
	 * directory name which contains the libraries
	 * 
	 * @param name
	 * @param version
	 * @throws IOException
	 */
	public NativeLibraryLoader(String name, String version) throws IOException {
		setup(name, version, null, false);
	}

	/**
	 * Defines the library. The name is used to identify the prefix of the resource
	 * directory name which contains the libraries
     *
	 * @param name
	 * @param version
	 * @param install
	 * @throws IOException
	 */
	public NativeLibraryLoader(String name, String version, boolean install) throws IOException {
		setup(name, version, null, install);
	}

	/**
	 * Constructor which is used for testing with a defined platform.
	 * 
	 * @param name
	 * @param version
	 * @param platform
	 * @throws IOException
	 */
	public NativeLibraryLoader(String name, String version, String platform) throws IOException {
		setup(name, version, platform, false);
	}

	private void setup(String name, String version, String platform, boolean install) throws IOException {
		// prefix for resource directory name
		this.name = name;
		// the version is used to make the copied artifact version dependent
		this.version = version == null ? "" : version;
		// os & processor
		this.platform = platform;

		this.supportsInstall = install;

		dependenciesRoot = parse();
	}

	/**
	 * Returns the root of the dependency tree
	 * 
	 * @return
	 */
	public DependencyTreeNode getDependenciesRoot() {
		return dependenciesRoot;
	}

	/**
	 * Loads the root library with installing any binaries
	 * 
	 * @throws IOException
	 */
	public void load() throws IOException {
		load(true);
	}

	/**
	 * Loads the root library
	 * 
	 * @throws IOException
	 */
	public void load(boolean install) throws IOException {
		// determine nodes
		List<DependencyTreeNode> nodes = getDependenciesRoot().getAllTreeNodes();
		DependencyTreeNode rootLibrary = nodes.get(0);

		if (!checkIfInstalled(rootLibrary)) {
			// install all libraries in the user space
			if (supportsInstall) {
				nodes.stream().filter(node -> node.getLibraryWithPath().startsWith("/usr"))
						.forEach(node -> this.createFile(node.getLibraryName()));
			}
		}

		// load the root library
		if (!this.loadLibrary(rootLibrary)) {
			throw new RuntimeException(this.lastException);
		}
	}

	/**
	 * We check if the dynamic library has been installed.
	 * 
	 * @param node
	 * @return true if all the binaries are available
	 * @throws IOException
	 */
	protected boolean checkIfInstalled(DependencyTreeNode node) throws IOException {
		if (Paths.get(node.getLibraryWithPath()).toFile().exists()) {
			return true;
		}
		String prefix = "cli/shogun/libshogun." + this.getDynamicLibraryExtension();
		Optional<File> file = this.findFile(prefix, new File(System.getenv("CONDA_PREFIX") + File.separator + "lib"),
				new File("/usr/local/opt"), new File("/usr/local/lib"), new File("/usr/lib"), new File("/usr"));
		if (file.isPresent()) {
			String path = file.get().toString();
			LOG.info("Using library " + path);
			node.setLibraryWithPath(path);
			return true;
		} else {
			LOG.warn("The library was not found: {}", node.getLibraryName());
			return false;
		}

	}

	/**
	 * Tries to load all dependencies (in reverse order)
	 */
	public void loadAll() {
		this.dependenciesRoot.getAllTreeNodesPostOrder().forEach(node -> this.loadLibrary(node));
	}

	/**
	 * Call to System.loadLibrary. If this fails we will need to call System.load()
	 * 
	 * @param library
	 * @return
	 */
	public boolean loadLibrary(DependencyTreeNode library) {
		// load() existing absolute path defined in lddtree.txt
		LOG.info("- loading: {}", library.getLibraryName());
		String libraryName = "";
		try {
			// load only if file exists. Otherwise we risk that the wrong library with an
			// identical name is loaded
			libraryName = library.getLibraryWithPath();
			if (new File(libraryName).exists()) {
				System.load(libraryName);
				LOG.info("- loaded: {}", libraryName);
				return true;
			}
		} catch (Throwable ex) {
			LOG.warn(libraryName + ": " + ex);
			lastException = ex;
		}

		// load() from temp direcoty. We only support libraries in the user space
		if (library.getLibraryWithPath().startsWith("/usr")) {
			try {
				libraryName = this.getTarget("") + File.separator + library.getLibraryName();
				if (new File(libraryName).exists()) {
					System.load(libraryName);
					LOG.info("- loaded: {}", library);
					return true;
				}
			} catch (Throwable ex) {
				LOG.warn(libraryName + ": " + ex);
				lastException = ex;
			}
		}

		// If the access with the path failed we call loadLibrary()
		try {
			if (library.isLoadLoadLibSupported()) {
				libraryName = library.getLibraryName();
				if (libraryName.startsWith("lib")) {
					if (os().equals("osx")) {
						int pos = libraryName.lastIndexOf(".");
						libraryName = libraryName.substring(3, pos);
					} else if (os().equals("linux")) {
						libraryName = libraryName.substring(3);
						libraryName = libraryName.replace(".so", "");
					}
				}
				System.loadLibrary(libraryName);
				LOG.info("- loaded: {}", libraryName);
				return true;
			}
		} catch (Throwable ex) {
			lastException = ex;
			LOG.warn("- Could not load: {}", libraryName);
		}
		return false;

	}

	/**
	 * Parse into a Dependency Tree
	 * 
	 * @param lines
	 * @return
	 */
	protected DependencyTreeNode parse() throws IOException {
		return parse(this.getLines());
	}

	/**
	 * Parse an array of string lines into a Dependency Tree
	 * 
	 * @param lines
	 * @return
	 */
	protected DependencyTreeNode parse(String[] lines) {
		int lastLevel = -1;
		DependencyTreeNode root = new DependencyTreeNode("", "");
		Stack<DependencyTreeNode> stack = new Stack();
		stack.push(root);
		for (String line : lines) {
			if (isLineValid(line)) {
				// ignore information after (
				int pos = line.indexOf("(");
				boolean supportLoadLib = true;
				if (pos >= 0) {
					supportLoadLib = !line.contains("*");
					line = line.substring(0, pos - 1);
				}

				String sa[] = line.split("=>");
				if (sa.length != 2) {
					throw new RuntimeException("Invalid line: " + line);
				}
				int level = getParseLevel(sa[0]);
				String name = sa[0].trim();
				String nameWithPath = sa[1].trim();
				DependencyTreeNode node = new DependencyTreeNode(name, nameWithPath);
				node.setLoadLoadLibSupported(supportLoadLib);
				if (level > lastLevel) {
					stack.peek().addChild(node);
					stack.push(node);
				} else if (level < lastLevel) {
					stack.pop();
					stack.peek().addChild(node);
				} else {
					stack.peek().addChild(node);
				}

				lastLevel = level;
			}
		}
		return root;
	}

	/**
	 * Count the leading spaces.
	 * 
	 * @param string
	 * @return
	 */
	protected int getParseLevel(String string) {
		int count = 0;
		for (char c : string.toCharArray()) {
			if (c != ' ') {
				break;
			}
			count++;
		}
		return count;
	}

	protected boolean isLineValid(String line) {
		return !line.trim().startsWith("//") && line.contains("=>");
	}

	/**
	 * Returns the list of libraries which need to be loaded
	 * 
	 * @return
	 * @throws IOException
	 */
	protected String[] getLines() throws IOException {
		String resourcePath = "/" + getPlatformSpecificName() + "/lddtree.txt";
		InputStream is = NativeLibraryLoader.class.getResourceAsStream(resourcePath);
		if (is == null) {
			throw new RuntimeException("Unsupported Platform: " + getPlatform());
		}
		String files = IOUtils.toString(is, "UTF8");
		return files.split("\\r?\\n");
	}

	/**
	 * Determines the file for the system library. We create the file from the
	 * resources if it does not exist
	 * 
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	protected File createFile(String resource) {
		File file = null;
		String resourcePath = "/" + getPlatformSpecificName() + "/" + resource;
		InputStream input = NativeLibraryLoader.class.getResourceAsStream(resourcePath.trim());
		if (input != null) {
			file = new File(getTarget(resource));
			if (this.isForceCopy() || !file.exists() || file.length() == 0) {
				try {
					FileUtils.copyInputStreamToFile(input, file);
					LOG.info("extracting {}", file.getAbsolutePath());
					// file.setExecutable(true, false);
					file.setReadable(true, false);
					file.setWritable(true, false);

				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			if (file == null || !file.exists() || file.length() == 0) {
				throw new RuntimeException("Error: File " + file + " not found!");
			}
		}

		return file;

	}

	/**
	 * Determines the location of the dynamic library. If the env variable
	 * DYNAMIC-LIBRARY-PATH is not defined we use the tmpdir
	 * 
	 * @param resource
	 * @return
	 */
	protected String getTarget(String resource) {
		String targetDirecotry = System.getenv("DYNAMIC-LIBRARY-PATH");
		if (targetDirecotry == null) {
			targetDirecotry = System.getProperty("java.io.tmpdir"); // "/Library/Java/Extensions";
		}
		StringBuffer result = new StringBuffer(targetDirecotry);
		if (!targetDirecotry.endsWith(File.separator)) {
			result.append(File.separator);
		}
		result.append(name);
		result.append("-");
		result.append(version);
		if (!resource.isEmpty()) {
			result.append(File.separator);
			result.append(resource);
		}

		return result.toString();
	}

	/**
	 * Concatenation of stem - OS - Architecture
	 * 
	 * @param stem
	 * @return
	 */
	public String getPlatformSpecificName() {
		return name + "-" + getPlatform();
	}

	/**
	 * Concatenation of OS - Architecture
	 * 
	 * @return
	 */
	protected String getPlatform() {
		if (platform == null) {
			String arch = arch();
			String os = os();
			this.platform = os + "-" + arch;
		}
		return this.platform;
	}

	/**
	 * Determines the architecture
	 * 
	 * @return
	 */
	protected String arch() {
		String arch = System.getProperty("os.arch", "").toLowerCase();
		if (arch.equals("amd64")) {
			arch = "x86_64";
		}
		return arch;
	}

	/**
	 * Determines the operation system
	 * 
	 * @return
	 */
	protected String os() {
		String os = System.getProperty("os.name", "").toLowerCase();
		if (os.startsWith("mac")) {
			os = "osx";
		}
		return os;
	}

	/**
	 * Determines if the dynamic library should be overwritten even if it already
	 * exists. Set to false to speed up the processing
	 * 
	 * @return
	 */
	public boolean isForceCopy() {
		return forceCopy;
	}

	/**
	 * Defines if the dynamic library should be overwritten even if it already
	 * exists. Set to false to speed up the processing
	 * 
	 * @param forceCopy
	 */
	public void setForceCopy(boolean forceCopy) {
		this.forceCopy = forceCopy;
	}

	/**
	 * Returns the platform specific name (used in the resources)
	 */
	public String toString() {
		return this.getPlatformSpecificName();
	}

	/**
	 * Finds (the first file) with the indicated path
	 * 
	 * @param regex
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public Optional<File> findFile(String suffix, File file) throws IOException {
		// cannot use Files.find because of AccessDeniedException
		try {
			if (file.isDirectory()) {
				LOG.debug(file.toString());
				File[] arr = file.listFiles();
				if (arr != null) {
					for (File f : arr) {
						Optional<File> found = findFile(suffix, f);
						if (found.isPresent()) {
							if (found.get().toString().endsWith(suffix)) {
								return found;
							}
						}
					}
				}
			} else {
				if (file.toString().endsWith(suffix)) {
					return Optional.of(file);
				}
			}
		} catch (Exception ex) {
			LOG.info(ex.getMessage());
		}
		return Optional.empty();
	}

	public Optional<File> findFile(String suffix, File... files) throws IOException {
		for (File file : files) {
			LOG.info("checking {}", file.toString());
			Optional<File> result = findFile(suffix, file);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns the extension of the dynamic library
	 * 
	 * @return
	 */
	public String getDynamicLibraryExtension() {
		String result = "";
		if (this.os().equals("osx")) {
			result = "jnilib";
		} else if (this.os().equals("linux")) {
			result = "so";
		} else if (this.os().equals("windows")) {
			result = "dll";
		}
		return result;
	}

}

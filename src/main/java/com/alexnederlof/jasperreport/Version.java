package com.alexnederlof.jasperreport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version implements Comparable<Version> {
	
	private static final Pattern VERSION_PATTERN
		= Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

	private final int major;
	private final int minor;
	private final int patch;

	public Version(int major, int minor, int patch) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	/**
	 * @throws Exception when str parameter contains malformed version.
	 *         Correct form is defined by regex "\d+\.\d+\.\d+".
	 */
	public static Version version(String str) throws Exception {

		Matcher matcher = VERSION_PATTERN.matcher(str);
		if (!matcher.matches()) {
			throw new Exception("Malformed version \"" + str + "\".");
		}
		
		return new Version(
			Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2)), Integer.valueOf(matcher.group(3)));
	}

	public static Version version(int major, int minor, int patch) {
		return new Version(major, minor, patch);
	}
	
	public int compareTo(Version o) {
		
		if (o == this) {
			return 0;
		}
		
		if (o == null) {
			return -1;
		}
		
		if (major == o.major) {
			if (minor == o.minor) {
				if (patch == o.patch) {
					return 0;
				}
				return patch > o.patch ? 1 : -1;
			}
			return minor > o.minor ? 1 : -1;
		}
		return major > o.major ? 1 : -1;
	}
}
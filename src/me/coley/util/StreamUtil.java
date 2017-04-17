package me.coley.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtil {
	public static Stream<String> sortJavaNames(Stream<String> stream) {
		return stream.sorted(new JavaNameSorter());
	}
	
	public static List<String> listOfSortedJavaNames(Collection<String> names){
		return sortJavaNames(names.stream()).collect(Collectors.toList());
	}
}

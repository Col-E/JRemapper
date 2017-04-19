package me.coley.jremap;

import java.io.IOException;
import java.util.Collection;

import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

public class CFRSourceImpl implements ClassFileSource {
	private final byte[] bytes;

	public CFRSourceImpl(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public void informAnalysisRelativePathDetail(String s, String s1) {}

	@Override
	public Collection<String> addJar(String s) {
		throw new UnsupportedOperationException("Return paths of all classfiles in jar.");
	}

	@Override
	public String getPossiblyRenamedPath(String s) {
		return s;
	}

	@Override
	public Pair<byte[], String> getClassFileContent(String s) throws IOException {
		return Pair.make(bytes, s);
	}
}

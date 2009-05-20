package net.solarvistas.android;

public class SyncAction {
	public String filename;
	public ScanFilesThread.Direction direction;
	public ModificationInfo modificationInfo;
	public SyncAction(String filename, ScanFilesThread.Direction direction, ModificationInfo modificationInfo) {
		this.filename = filename; this.direction = direction; this.modificationInfo = modificationInfo;
	}
}

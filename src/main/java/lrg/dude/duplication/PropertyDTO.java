package lrg.dude.duplication;

public class PropertyDTO {
	public PropertyDTO(String file, String name, String category, int value) {
		this.file = file;
		this.name = name;
		this.category = category;
		this.value = value;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
		this.value = value;
	}
	private String file;
	private String name;
	private String category;
	private int value;
}
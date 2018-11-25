public class EbayItem {
	private String name;
	private String itemURL;
	private String imageURL;
	private String price;
	private String currency;

	public EbayItem() {
		
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setItemURL(String itemURL) {
		this.itemURL= itemURL;
	}
	
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	
	public void setPrice(String price) {
		this.price = price;
	}
	
	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getName() {
		return name;
	}
	
	public String getItemURL() {
		return itemURL;
	}
	
	public String getImageURL() {
		return imageURL;
	}
	
	public String getPrice() {
		return price;
	}
	
	public String getCurrency() {
		return currency;
	}
}

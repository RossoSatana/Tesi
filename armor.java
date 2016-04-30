package item;

public class armor extends Item{
	private double fDef;
	private double mDef;
	
	public armor(double f, double m, String n, int v){
		super(n, v);
		fDef = f;
		mDef= m;
	}

	public double getfDef() {
		return fDef;
	}
	public void setfDef(double fDef) {
		this.fDef = fDef;
	}
	public double getmDef() {
		return mDef;
	}
	public void setmDef(double mDef) {
		this.mDef = mDef;
	}

}

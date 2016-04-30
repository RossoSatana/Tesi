package item;

public class weapon extends Item{
	private double fDmg;
	private double mDmg;

	public weapon(double f, double m, String n, int v){
		super(n, v);
		fDmg=f;
		mDmg=m;
	}
	public double getfDmg() {
		return fDmg;
	}
	public void setfDmg(double fDmg) {
		this.fDmg = fDmg;
	}
	public double getmDmg() {
		return mDmg;
	}
	public void setmDmg(double mDmg) {
		this.mDmg = mDmg;
	}

}

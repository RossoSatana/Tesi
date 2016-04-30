package Monsters;

import item.armor;
import item.body;
import item.shoes;

public class battle {
	public static final void main(String[] args){

		mago A1 = new mago();
		shoes g = new shoes (20, 23, null, 0);
		A1.equip(g);
		
		body f = new body(50, 60, "Vessillo", 0);
		A1.equip(f);
	
	}
}

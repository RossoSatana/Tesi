package Monsters;

import item.armor;
import item.body;
import item.helm;
import item.shoes;
import item.weapon;

public abstract class mostri {
	private setEquip equipment = new setEquip();
	private String name, type1, type2 ;
	int exp = 0;
	int lvl = 1;
	int atk, spatk, def, spdef,  hp, mag;

	public mostri(String n, String t1, String t2, int a, int d, int h, int m, int spa, int spd){
		name=n;	type1=t1; type2=t2;
		atk=a; spatk=spa;
		def=d; spdef=spd;
		hp=h; mag=m;
	}

	/*mostra esperienza*/
	public int getExp(){
		return exp;
	}

	/*equipaggiamento armatura*/
	public void equip(armor a){
		if (a instanceof helm)
			equipment.setElm((helm)a);
		if (a instanceof body)
			equipment.setBod((body)a);
		if (a instanceof shoes)
			equipment.setSho((shoes)a);
	}
	/*equipaggiamento arma*/
	public void equip(weapon a){
		equipment.setWep(a);

	}
	/*mostra armatura*/
	public helm showAH(){
		return equipment.getElm();	
	}
	public shoes showAS(){
		return equipment.getSho();	
	}
	public body showAB(){
		return equipment.getBod();	
	}
	/*mostra arma*/
	public weapon showW(){
		return equipment.getWep();	
	}

}

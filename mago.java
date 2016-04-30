package Monsters;

public class mago extends mostri {	
	public mago(){
		super ("Mago", "malee", "elemento", 6, 7, 70, 14, 12, 10);
	}

	public void lvlUp(){
		hp += 120;
		atk += 60;
		def += 60;
		mag += 60;
		spdef += 60;
		spatk += 60;
		lvl++;
	}
}



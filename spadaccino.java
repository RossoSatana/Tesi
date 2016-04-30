package Monsters;

public class spadaccino extends mostri {
	public spadaccino(){
		super ("Spadaccino", "malee", "elemento", 10, 12, 140, 6, 8, 9);
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

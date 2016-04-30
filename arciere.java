package Monsters;

public class arciere extends mostri {
	public arciere(){
		super ("Arciere", "ranged", "elemento", 13, 6, 80, 9, 8, 6);
	}

	public void lvlUp(){
		hp += 60;
		atk += 60;
		def += 60;
		mag += 60;
		spdef += 60;
		spatk += 60;
		lvl++;
	}
}
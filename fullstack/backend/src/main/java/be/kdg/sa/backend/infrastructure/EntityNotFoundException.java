package be.kdg.sa.backend.infrastructure;


public class EntityNotFoundException extends RuntimeException {
	public EntityNotFoundException(Object o){
		super("Entity not found: " + o.toString());
	}
}

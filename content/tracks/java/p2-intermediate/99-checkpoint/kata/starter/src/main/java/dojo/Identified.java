package dojo;

/** Anything stored by id in a repository. */
public interface Identified<ID> {
    ID id();
}

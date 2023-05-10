package edu.kit.datamanager.pit.pidsystem;

import edu.kit.datamanager.pit.common.InvalidConfigException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.TypeDefinition;
import edu.kit.datamanager.pit.pidgeneration.PidSuffix;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * Main abstraction interface towards the identifier system containing
 * registered identifiers and associated state information.
 *
 */
public interface IIdentifierSystem {

    /**
     * Returns the configured prefix of this PID system.
     * 
     * If this system can create PIDs, the prefix is the one it uses to create PIDs.
     * Otherwise, it does not return a prefix.
     * 
     * @return the prefix this system uses to create PIDs, if it can create PIDs,
     *         empty otherwise.
     */
    public Optional<String> getPrefix();

    /**
     * Appends the given PID to the prefix, if possible.
     * 
     * It may not be possible if no prefix is present, or if the PID already starts
     * with a the prefix. The returnes String is then exaxtly the same.
     * 
     * @param pid the PID to append to the prefix.
     * @return the PID with the prefix appended, if possible.
     * @throws InvalidConfigException if the system can n.
     */
    public default String appendPrefixIfAbsent(String pid) throws InvalidConfigException {
        Optional<String> prefix = this.getPrefix();
        if (prefix.isPresent() && !pid.startsWith(prefix.get())) {
            return new PidSuffix(pid).getWithPrefix(prefix.get());
        } else {
            return pid;
        }
    }

    /**
     * Checks whether the given PID is already registered.
     *
     * @param pid the PID to check.
     * @return true, if the PID is registered, false otherwise.
     * @throws IOException if the check could not be performed.
     */
    public boolean isIdentifierRegistered(String pid) throws IOException;

    /**
     * Checks whether the given PID is already registered.
     * 
     * Assumes the PID to be the configured prefix of the system combined with the given suffix.
     * 
     * @param suffix the given suffix, which, appended to the configured prefix, forms the PID to check.
     * @return true, if the PID is registered, false otherwise.
     * @throws IOException if the check could not be performed.
     */
    public default boolean isIdentifierRegistered(PidSuffix suffix) throws IOException, InvalidConfigException {
        String prefix = getPrefix().orElseThrow(() -> new InvalidConfigException("This system cannot create PIDs."));
        return isIdentifierRegistered(suffix.getWithPrefix(prefix));
    }

    /**
     * Queries all properties from the given PID, independent of types.
     *
     * @param pid
     * @return a PID information record with its PID and attribute-value-pairs.
     * The property names will be empty strings. Contains all property
     * values present in the record of the given PID. If the pid is not
     * registered, the method returns null.
     * @throws IOException
     */
    public PIDRecord queryAllProperties(String pid) throws IOException;

    /**
     * Queries a single property from the given PID.
     *
     * @param pid
     * @param typeDefinition
     * @return the property value or null if there is no property of given name
     * defined in this PID record.
     * @throws IOException
     */
    public String queryProperty(String pid, TypeDefinition typeDefinition) throws IOException;

    /**
     * Registers a new PID with given property values. The method takes the PID from
     * the record and treats it as a suffix.
     * 
     * The method must process the given PID using the
     * {@link #registerPID(PIDRecord)} method.
     *
     * @param record contains the initial PID record.
     * @return the PID that was assigned to the record.
     * @throws IOException
     */
    public default String registerPID(final PIDRecord pidRecord) throws IOException, RecordValidationException {
        if (pidRecord.getPid() == null) {
            throw new RecordValidationException("<null>", "PID must not be null.");
        }
        if (pidRecord.getPid().isEmpty()) {
            throw new RecordValidationException("<empty>", "PID must not be empty.");
        }
        pidRecord.setPid(
            appendPrefixIfAbsent(pidRecord.getPid())
        );
        return registerPidUnchecked(pidRecord);
    }

    /**
     * Registers the given record with its given PID, without applying any checks.
     * Recommended to use {@link #registerPID(PIDRecord)} instead.
     * 
     * As an implementor, you can assume the PID to be not null, valid,
     * non-registered, and prefixed.
     * 
     * @param pidRecord the record to register.
     * @return the PID that was assigned to the record.
     * @throws IOException if the PID could not be registered.
     */
    public String registerPidUnchecked(final PIDRecord pidRecord) throws IOException;

    /**
     * Updates an existing record with the new given values.
     * If the PID in the given record is not valid, it will return false.
     * 
     * @param record Assumes an existing, valid PID inside this record.
     * @return false if there was no existing, valid PID in this record.
     * @throws IOException
     */
    public boolean updatePID(PIDRecord record) throws IOException;

    /**
     * Queries all properties of a given type available from the given PID. If
     * optional properties are present, they will be returned as well. If there
     * are mandatory properties missing (i.e. the record of the given PID does
     * not fully conform to the type), the method will NOT fail but simply
     * return only those properties that are present.
     *
     * @param pid
     * @param typeDefinition
     * @return a PID information record with property identifiers mapping to
     * values. The property names will not be available (empty Strings).
     * Contains all property values present in the record of the given PID that
     * are also specified by the type (mandatory or optional). If the pid is not
     * registered, the method returns null.
     * @throws IOException
     */
    public PIDRecord queryByType(String pid, TypeDefinition typeDefinition) throws IOException;

    /**
     * Remove the given PID. Obviously, this method is only for testing
     * purposes, since we should not delete persistent identifiers...
     *
     * @param pid
     * @return true if the identifier was deleted, false if it did not exist
     */
    public boolean deletePID(String pid) throws IOException;

    /**
     * Returns all PIDs which are registered for the configured prefix.
     * 
     * The result may be very large, use carefully.
     * 
     * @return all PIDs which are registered for the configured prefix.
     */
    public Collection<String> resolveAllPidsOfPrefix() throws IOException, InvalidConfigException;
}

package edu.kit.datamanager.pit.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import edu.kit.datamanager.pit.pitservice.ITypingService;

/**
 * Simple operations on PID records.
 * 
 * Caches results e.g. for type queries
 */
public class Operations {

    private static final String[] KNOWN_DATE_CREATED = {
        "21.T11148/29f92bd203dd3eaa5a1f",
        "21.T11148/aafd5fb4c7222e2d950a"
    };

    private static final String[] KNOWN_DATE_MODIFIED = {
        "21.T11148/397d831aa3a9d18eb52c"
    };

    private static final List<String> KNOWN_DIGITAL_OBJECT_TYPE_ATTRIBUTES = List.of(
        "21.T11148/1c699a5d1b4ad3ba4956"
    );

    private static final List<String> KNOWN_SUPPORTED_TYPES = List.of(
        "21.T11148/2694e4a7a5a00d44e62b"
    );

    private static final List<String> KNOWN_SUPPORTED_LOCATIONS = List.of();

    private ITypingService typingService;

    public Operations(ITypingService typingService) {
        this.typingService = typingService;
    }

    public Set<String> findDigitalObjectTypes(PIDRecord pidRecord) throws IOException {
        Set<String> doTypes = KNOWN_DIGITAL_OBJECT_TYPE_ATTRIBUTES
            .stream()
            .map(pidRecord::getPropertyValues)
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .collect(Collectors.toSet());
        if (!doTypes.isEmpty()) {
            return doTypes;
        }

        /* TODO try to find types extending or relating otherwise to known types
         *      (currently not supported by our TypeDefinition) */
        // we need to resolve types without streams to forward possible exceptions
        Collection<TypeDefinition> resolvedAttributeDescriptions = new ArrayList<>();
        for (String attributePid : pidRecord.getPropertyIdentifiers()) {
            if (this.typingService.isIdentifierRegistered(attributePid)) {
                TypeDefinition type = this.typingService.describeType(attributePid);
                resolvedAttributeDescriptions.add(type);
            }
        }

        /*
         * as a last fallback, try find types with human readable names containing
         * "digitalObjectType".
         * 
         * This can be removed as soon as we have some default FAIR DO types new type
         * definitions can refer to (e.g. "extend" them or declare the same meaning as
         * our known types, see above)
         */
        return resolvedAttributeDescriptions
            .stream()
            .filter(type -> type.getName().equals("digitalObjectType"))
            .map(type -> pidRecord.getPropertyValues(type.getIdentifier()))
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .collect(Collectors.toSet());
    }

    /**
     * Tries to get supported types. Usually, this property only exists for FAIR DOs
     * representing operations.
     * 
     * Strategy: - try to get it from known "dateModified" types
     * 
     * Semantic reasoning in some sense is planned but not yet supported.
     * 
     * @param pidRecord the record to extract the information from.
     * @return the extracted "supported types", if any.
     */
    public Set<String> findSupportedTypes(PIDRecord pidRecord) {
        return KNOWN_SUPPORTED_TYPES
            .stream()
            .map(pidRecord::getPropertyValues)
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .collect(Collectors.toSet());
    }

    /**
     * Tries to get supported types. Usually, this property only exists for FAIR DOs
     * representing operations.
     * 
     * Strategy: - try to get it from known "dateModified" types
     * 
     * Semantic reasoning in some sense is planned but not yet supported.
     * 
     * @param pidRecord the record to extract the information from.
     * @return the extracted "supported locations", if any.
     */
    public Set<String> findSupportedLocations(PIDRecord pidRecord) {
        return KNOWN_SUPPORTED_LOCATIONS
            .stream()
            .map(pidRecord::getPropertyValues)
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .collect(Collectors.toSet());
    }

    /**
     * Tries to get the date when a FAIR DO was created from a PID record.
     * 
     * Strategy:
     * - try to get it from known "dateCreated" types
     * - as a fallback, try to get it by its human readable name
     * 
     * Semantic reasoning in some sense is planned but not yet supported.
     * 
     * @param pidRecord the record to extract the information from.
     * @return the date, if it could been extracted.
     * @throws IOException on IO errors regarding resolving types.
     */
    public Optional<Date> findDateCreated(PIDRecord pidRecord) throws IOException {
        /* try known types */
        List<String> knownDateTypes = Arrays.asList(Operations.KNOWN_DATE_CREATED);
        Optional<Date> date = knownDateTypes
            .stream()
            .map(pidRecord::getPropertyValues)
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .map(this::extractDate)
            .filter(Optional<Date>::isPresent)
            .map(Optional<Date>::get)
            .sorted(Comparator.comparingLong(Date::getTime))
            .findFirst();
        if (date.isPresent()) {
            return date;
        }

        /* TODO try to find types extending or relating otherwise to known types
         *      (currently not supported by our TypeDefinition) */
        // we need to resolve types without streams to forward possible exceptions
        Collection<TypeDefinition> types = new ArrayList<>();
        for (String attributePid : pidRecord.getPropertyIdentifiers()) {
            if (this.typingService.isIdentifierRegistered(attributePid)) {
                TypeDefinition type = this.typingService.describeType(attributePid);
                types.add(type);
            }
        }

        /*
         * as a last fallback, try find types with human readable names containing
         * "dateCreated" or "createdAt" or "creationDate".
         * 
         * This can be removed as soon as we have some default FAIR DO types new type
         * definitions can refer to (e.g. "extend" them or declare the same meaning as
         * our known types, see above)
         */
        return types
            .stream()
            .filter(type -> 
                type.getName().equals("dateCreated")
                || type.getName().equals("createdAt")
                || type.getName().equals("creationDate"))
            .map(type -> pidRecord.getPropertyValues(type.getIdentifier()))
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .map(this::extractDate)
            .filter(Optional<Date>::isPresent)
            .map(Optional<Date>::get)
            .sorted(Comparator.comparingLong(Date::getTime))
            .findFirst();
    }

    /**
     * Tries to get the date when a FAIR DO was modified from a PID record.
     * 
     * Strategy:
     * - try to get it from known "dateModified" types
     * - as a fallback, try to get it by its human readable name
     * 
     * Semantic reasoning in some sense is planned but not yet supported.
     * 
     * @param pidRecord the record to extract the information from.
     * @return the date, if it could been extracted.
     * @throws IOException on IO errors regarding resolving types.
     */
    public Optional<Date> findDateModified(PIDRecord pidRecord) throws IOException {
        /* try known types */
        List<String> knownDateTypes = Arrays.asList(Operations.KNOWN_DATE_MODIFIED);
        Optional<Date> date = knownDateTypes
            .stream()
            .map(pidRecord::getPropertyValues)
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .map(this::extractDate)
            .filter(Optional<Date>::isPresent)
            .map(Optional<Date>::get)
            .sorted(Comparator.comparingLong(Date::getTime))
            .findFirst();
        if (date.isPresent()) {
            return date;
        }

        /* TODO try to find types extending or relating otherwise to known types
         *      (currently not supported by our TypeDefinition) */
        // we need to resolve types without streams to forward possible exceptions
        Collection<TypeDefinition> types = new ArrayList<>();
        for (String attributePid : pidRecord.getPropertyIdentifiers()) {
            if (this.typingService.isIdentifierRegistered(attributePid)) {
                TypeDefinition type = this.typingService.describeType(attributePid);
                types.add(type);
            }
        }

        /*
         * as a last fallback, try find types with human readable names containing
         * "dateModified" or "lastModified" or "modificationDate".
         * 
         * This can be removed as soon as we have some default FAIR DO types new type
         * definitions can refer to (e.g. "extend" them or declare the same meaning as
         * our known types, see above)
         */
        return types
            .stream()
            .filter(type -> 
                type.getName().equals("dateModified")
                || type.getName().equals("lastModified")
                || type.getName().equals("modificationDate"))
            .map(type -> pidRecord.getPropertyValues(type.getIdentifier()))
            .map(Arrays::asList)
            .flatMap(List<String>::stream)
            .map(this::extractDate)
            .filter(Optional<Date>::isPresent)
            .map(Optional<Date>::get)
            .sorted(Comparator.comparingLong(Date::getTime))
            .findFirst();
    }

    /**
     * Tries to extract a Date object from a String.
     * 
     * @param dateString the date string to extract the date from.
     * @return the extracted Date object.
     */
    protected Optional<Date> extractDate(String dateString) {
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
        try {
            DateTime dateTime = dateFormatter.parseDateTime(dateString);
            return Optional.of(dateTime.toDate());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

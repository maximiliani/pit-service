    package edu.kit.datamanager.pit.typeregistry.impl;

import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.typeregistry.schema.SchemaInfo;
import edu.kit.datamanager.pit.typeregistry.schema.SchemaSetGenerator;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TypeApiTest {

    // PID of checksum type in dtr-test. Currently (Dec 2024), the schema that type-api generated is malformed.
    public static final String PID_COMPLEX_TYPE_CHECKSUM_DTRTEST = "21.T11148/82e2503c49209e987740";
    private final TypeApi dtr;

    TypeApiTest() throws MalformedURLException, URISyntaxException {
        ApplicationProperties props = new ApplicationProperties();
        // set cache properties
        props.setExpireAfterWrite(10);
        props.setMaximumSize(1000);
        // set type registry
        props.setTypeRegistryUri(new URI("https://typeapi.lab.pidconsortium.net").toURL());
        props.setHandleBaseUri(new URI("https://hdl.handle.net").toURL());
        this.dtr = new TypeApi(props, new SchemaSetGenerator(props));
    }

    @Test
    void querySchemaOfComplexType() {
        // NOTE The new Type-API currently returns a malformed schema for the
        //  checksum type in dtr-test. This test ensures that we at least get
        //  the legacy schema in this case. If this test breaks, we either
        //  have no schema, or the type-api is fixed.
        Set<SchemaInfo> s = dtr.querySchemas(PID_COMPLEX_TYPE_CHECKSUM_DTRTEST);
        assertEquals(2, s.size());
        Optional<SchemaInfo> result = s.stream()
                .filter(schemaInfo -> schemaInfo.schema() != null)
                .filter(schemaInfo -> schemaInfo.error() == null)
                .filter(schemaInfo -> schemaInfo.origin().contains("dtr-test"))
                .findAny();
        assertTrue(result.isPresent());
    }
}
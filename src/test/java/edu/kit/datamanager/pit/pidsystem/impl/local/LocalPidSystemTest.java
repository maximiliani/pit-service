package edu.kit.datamanager.pit.pidsystem.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystemTest;

/**
 * This tests the same things as `IIdentifierSystemTest`, but is separated from
 * it as it is not possible to have all the spring bean magic in a static
 * method, which is required to prepare the parameterized test cases.
 */
// JUnit5 + Spring
@SpringBootTest
// Set the in-memory implementation
@TestPropertySource(
    locations = "/test/application-test.properties",
    properties = "pit.pidsystem.implementation=LOCAL"
)
@ActiveProfiles("test")
public class LocalPidSystemTest {
    IIdentifierSystemTest systemTests = new IIdentifierSystemTest();
    
    @Autowired
    IIdentifierSystem localPidSystem;
    
    @Test
    void testConfig() {
        assertNotNull(localPidSystem);
    }
    
    @Test
    @Transactional
    void testAllSystemTests() throws Exception {
        PIDRecord rec = new PIDRecord();
        rec.addEntry(
            // this is actually a registered type, but not in a data type registry, but inline in the PID system.
            "10320/loc",
            "",
            "objects/21.T11148/076759916209e5d62bd5\" weight=\"1\" view=\"json\""
            + "#objects/21.T11148/076759916209e5d62bd5\" weight=\"0\" view=\"ui\""
        );
        //rec.addEntry("10320/loc", "", "value");
        String pid = localPidSystem.registerPID(rec);
        assertEquals(rec.getPid(), pid);
        PIDRecord newRec = localPidSystem.queryAllProperties(pid);
        assertEquals(rec, newRec);
        
        Set<Method> publicMethods = new HashSet<>(Arrays.asList(IIdentifierSystemTest.class.getMethods()));
        Set<Method> allDirectMethods = new HashSet<>(Arrays.asList(IIdentifierSystemTest.class.getDeclaredMethods()));
        publicMethods.retainAll(allDirectMethods);
        assertEquals(7, publicMethods.size());
        for (Method test : publicMethods) {
            int numParams = test.getParameterCount();
            if (numParams == 2) {
                try {
                    test.invoke(systemTests, localPidSystem, rec.getPid());
                } catch (Exception e) {
                    System.err.println(String.format("Test: %s", test));
                    System.err.println(String.format("Exception: %s", e));
                    throw e;
                }
            } else if (numParams == 3) {
                test.invoke(systemTests, localPidSystem, rec.getPid(), "sandboxed/NONEXISTENT");
            } else if (numParams == 0) {
                // This is not a test but some kind of helper or static method.
            } else {
                throw new Exception("There was a method with an unexpected amount of parameters. Handle this case here.");
            }
        }
    }
}
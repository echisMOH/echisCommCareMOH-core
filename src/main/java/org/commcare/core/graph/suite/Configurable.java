package org.commcare.core.graph.suite;

import org.commcare.suite.model.Text;

import java.util.Enumeration;

/**
 * Interface to be implemented by any classes in this package that store configuration data using a String => Text mapping.
 *
 * @author jschweers
 */
public interface Configurable {

    Enumeration getConfigurationKeys();

    Text getConfiguration(String key);

    void setConfiguration(String key, Text value);

}

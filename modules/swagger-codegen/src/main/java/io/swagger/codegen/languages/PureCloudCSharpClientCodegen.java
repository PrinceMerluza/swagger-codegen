package io.swagger.codegen.languages;

import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenProperty;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

public class PureCloudCSharpClientCodegen extends CSharpClientCodegen {

    protected Logger LOGGER = LoggerFactory.getLogger(PureCloudCSharpClientCodegen.class);

    public PureCloudCSharpClientCodegen() {
        super();

        // Use C# templates
        embeddedTemplateDir = templateDir = "purecloud" + File.separator + "csharp";

        // Prevents collision between System.Attribute and ININ.PureCloudApi.Model.Attribute
        typeMapping.put("Attribute", "ININ.PureCloudApi.Model.Attribute");

        importMapping.remove("LocalDateTime");
    }

    @Override
    public String getName() {
        return "purecloudcsharp";
    }

    @Override
    /**
     * Get the value of x-inin-method-name, or use default C# behavior if blank.
     *
     * @param operation the operation object
     * @param path the path of the operation
     * @param httpMethod the HTTP method of the operation
     * @return the (generated) operationId
     */
    protected String getOrGenerateOperationId(Operation operation, String path, String httpMethod) {
        if (operation.getVendorExtensions().containsKey("x-inin-method-name")) {
            String ininMethodName = operation.getVendorExtensions().get("x-inin-method-name").toString();
            if (!StringUtils.isBlank(ininMethodName)) return ininMethodName;
        }

        return super.getOrGenerateOperationId(operation, path, httpMethod);
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        // Execute super method
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);

        // Use our own values for hasMore
        boolean foundLastValidProperty = false;
        for (int i = codegenModel.vars.size() -1; i >= 0; i--) {
            CodegenProperty cp = codegenModel.vars.get(i);

            // If we've found the last property already, set it and move on
            if (foundLastValidProperty) {
                cp.hasMore = true;

                // Prevent trailing commas from readonly props
                if (cp.isReadOnly != null && cp.isReadOnly) {
                    cp.hasMoreNonReadOnly = null;
                } else {
                    cp.hasMoreNonReadOnly = true;
                }

                continue;
            }

            // If the property isn't readonly, we've found the last valid property
            if (cp.isReadOnly == null || !cp.isReadOnly){
                foundLastValidProperty = true;
                cp.hasMore = null;
                cp.hasMoreNonReadOnly = null;
                continue;
            }
        }

        // Make sure last property in list doesn't think there's more
        if (codegenModel.vars.size() > 0)
            codegenModel.vars.get(codegenModel.vars.size()-1).hasMore = null;

        // Set hasRequired only if non-readonly properties are required
        codegenModel.hasRequired = null;
        for (CodegenProperty cp : codegenModel.vars) {
            if (cp.isReadOnly == null && cp.required != null && cp.required) {
                codegenModel.hasRequired = true;
                break;
            }
        }

        return codegenModel;
    }
}

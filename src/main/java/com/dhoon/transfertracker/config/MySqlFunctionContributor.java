package com.dhoon.transfertracker.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

public class MySqlFunctionContributor implements FunctionContributor {


    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {

        var typeConfiguration = functionContributions.getTypeConfiguration();
        var doubleType = typeConfiguration
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.DOUBLE);


        functionContributions
                .getFunctionRegistry()
                .registerPattern(
                        "match_against",
                        "match(?1, ?2) against (?3 in boolean mode)",
                        doubleType
                );
    }


}

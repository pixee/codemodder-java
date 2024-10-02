package com.mycompany.app.jndi;

import jakarta.ws.rs.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Path("/unsafe-jndi-lookup")
public class JNDIVuln {

    @GET
    public String lookupResource(@QueryParam("resource") final String resource) throws NamingException {
        Context ctx = new InitialContext();
        Object obj = ctx.lookup(resource);
        return String.valueOf(obj);
    }

    @POST
    public String lookupAnotherResource(@QueryParam("resource") final String resource) throws NamingException {
        return FindResource.findResource(resource);
    }

    @PUT
    public String lookupYetAnotherResource(@QueryParam("resource") final String resource) throws NamingException {
        return FindResource.findResource(resource);
    }
}

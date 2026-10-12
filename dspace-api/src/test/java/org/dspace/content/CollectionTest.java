/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import org.dspace.authorize.AuthorizeException;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.*;
import mockit.Mocked;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

/**
 * Unit Tests for class Collection
 * @author pvillega
 */
public class CollectionTest extends AbstractDSpaceObjectTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(CollectionTest.class);

    /**
     * Collection instance for the tests
     */
    private Collection c;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
        try
        {
            //we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            this.c = Collection.create(context);
            this.dspaceObject = c;
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
            context.commit();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init");
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init");
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        c = null;
        super.destroy();
    }

    /**
     * Test of find method, of class Collection.
     */
    @Test
    public void testCollectionFind() throws Exception
    {
        int id = c.getID();
        Collection found =  Collection.find(context, id);
        assertThat("testCollectionFind 0", found, notNullValue());
        assertThat("testCollectionFind 1", found.getID(), equalTo(id));
        //the community created by default has no name
        assertThat("testCollectionFind 2", found.getName(), equalTo(""));
    }

    /**
     * Test of create method, of class Collection.
     */
    @Test
    public void testCreate() throws Exception
    {
        Collection created = Collection.create(context);
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), equalTo(""));
    }

     /**
     * Test of create method (with specified valid handle), of class Collection
     */
    @Test
    public void testCreateWithValidHandle() throws Exception
    {
        // test creating collection with a specified handle which is NOT already in use
        // (this handle should not already be used by system, as it doesn't start with "1234567689" prefix)
        Collection created = Collection.create(context, "987654321/100");

        // check that collection was created, and that its handle was set to proper value
        assertThat("testCreateWithValidHandle 0", created, notNullValue());
        assertThat("testCreateWithValidHandle 1", created.getHandle(), equalTo("987654321/100"));
    }


     /**
     * Test of create method (with specified invalid handle), of class Collection.
     */
    @Test(expected=IllegalStateException.class)
    public void testCreateWithInvalidHandle() throws Exception
    {
        //get handle of our default created collection
        String inUseHandle = c.getHandle();

        // test creating collection with a specified handle which IS already in use
        // This should throw an exception
        Collection created = Collection.create(context, inUseHandle);
        fail("Exception expected");
    }


    /**
     * Test of findAll method, of class Collection.
     */
    @Test
    public void testFindAll() throws Exception
    {
        Collection[] all = Collection.findAll(context);
        assertThat("testFindAll 0", all, notNullValue());
        assertTrue("testFindAll 1", all.length >= 1);

        boolean added = false;
        for(Collection cl: all)
        {
            if(cl.equals(c))
            {
                added = true;
            }
        }
        assertTrue("testFindAll 2",added);
    }

    /**
     * Test of getItems method, of class Collection.
     */
    @Test
    public void testGetItems() throws Exception
    {
        ItemIterator items = c.getItems();
        assertThat("testGetItems 0", items, notNullValue());
        //by default is empty
        assertFalse("testGetItems 1", items.hasNext());
        assertThat("testGetItems 2", items.next(), nullValue());
        assertThat("testGetItems 3", items.nextID(), equalTo(-1));
    }

    /**
     * Test of getAllItems method, of class Collection.
     */
    @Test
    public void testGetAllItems() throws Exception
    {
        ItemIterator items = c.getAllItems();
        assertThat("testGetAllItems 0", items, notNullValue());
        //by default is empty
        assertFalse("testGetAllItems 1", items.hasNext());
        assertThat("testGetAllItems 2", items.next(), nullValue());
        assertThat("testGetAllItems 3", items.nextID(), equalTo(-1));
    }

    /**
     * Test of getID method, of class Collection.
     */
    @Test
    @Override
    public void testGetID() 
    {
        assertTrue("testGetID 0", c.getID() >= 1);
    }

    /**
     * Test of getHandle method, of class Collection.
     */
    @Test
    @Override
    public void testGetHandle() 
    {
        //default instance has a random handle
        assertTrue("testGetHandle 0", c.getHandle().contains("123456789/"));
    }

    /**
     * Test of getMetadata method, of class Collection.
     */
    @Test
    public void testGetMetadata()
    {
        //by default all empty values will return ""
        assertThat("testGetMetadata 0",c.getMetadata("name"), equalTo(""));
        assertThat("testGetMetadata 1",c.getMetadata("short_description"), equalTo(""));
        assertThat("testGetMetadata 2",c.getMetadata("introductory_text"), equalTo(""));
        assertThat("testGetMetadata 3",c.getMetadata("logo_bitstream_id"), equalTo(""));
        assertThat("testGetMetadata 4",c.getMetadata("copyright_text"), equalTo(""));
        assertThat("testGetMetadata 5",c.getMetadata("template_item_id"), equalTo(""));
        assertThat("testGetMetadata 6",c.getMetadata("provenance_description"), equalTo(""));
        assertThat("testGetMetadata 7",c.getMetadata("side_bar_text"), equalTo(""));
        assertThat("testGetMetadata 8",c.getMetadata("license"), equalTo(""));
    }

    /**
     * Test of setMetadata method, of class Collection.
     */
    @Test
    public void testSetMetadata()
    {
        String name = "name";
        String sdesc = "short description";
        String itext = "introductory text";
        String logo = "1";
        String copy = "copyright declaration";
        String sidebar = "side bar text";
        String tempItem = "3";
        String provDesc = "provenance description";
        String license = "license text";

        c.setMetadata("name", name);
        c.setMetadata("short_description", sdesc);
        c.setMetadata("introductory_text", itext);
        c.setMetadata("logo_bitstream_id", logo);
        c.setMetadata("copyright_text", copy);
        c.setMetadata("side_bar_text", sidebar);
        c.setMetadata("template_item_id", tempItem);
        c.setMetadata("provenance_description", provDesc);
        c.setMetadata("license", license);

        assertThat("testSetMetadata 0",c.getMetadata("name"), equalTo(name));
        assertThat("testSetMetadata 1",c.getMetadata("short_description"), equalTo(sdesc));
        assertThat("testSetMetadata 2",c.getMetadata("introductory_text"), equalTo(itext));
        assertThat("testSetMetadata 3",c.getMetadata("logo_bitstream_id"), equalTo(logo));
        assertThat("testSetMetadata 4",c.getMetadata("copyright_text"), equalTo(copy));
        assertThat("testSetMetadata 5",c.getMetadata("side_bar_text"), equalTo(sidebar));
        assertThat("testGetMetadata 6",c.getMetadata("template_item_id"), equalTo(tempItem));
        assertThat("testGetMetadata 7",c.getMetadata("provenance_description"), equalTo(provDesc));
        assertThat("testGetMetadata 8",c.getMetadata("license"), equalTo(license));
    }

    /**
     * Test of getName method, of class Collection.
     */
    @Test
    @Override
    public void testGetName()
    {
        //by default is empty
        assertThat("testGetName 0",c.getName(), equalTo(""));
    }

    /**
     * Test of getLogo method, of class Collection.
     */
    @Test
    public void testGetLogo()
    {
        //by default is empty
        assertThat("testGetLogo 0",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD, true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE, true); result = true;
                        minTimes = 0;
                 AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE, true); result = null;
                        minTimes = 0;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        assertThat("testSetLogoAuth 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth2(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                 AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        assertThat("testSetLogoAuth2 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth2 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth3(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                 AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        assertThat("testSetLogoAuth3 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth3 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth4(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                 AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        assertThat("testSetLogoAuth4 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth4 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testSetLogoNoAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        fail("EXception expected");
    }

    /**
     * Test of createWorkflowGroup method, of class Collection.
     */
    @Test
    public void testCreateWorkflowGroupAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageWorkflowsGroup((Context) any, (Collection) any);
                    result = null;
                    minTimes = 0;
            }
        };

        int step = 1;
        Group result = c.createWorkflowGroup(step);
        assertThat("testCreateWorkflowGroupAuth 0", result, notNullValue());
    }

    /**
     * Test of createWorkflowGroup method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateWorkflowGroupNoAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageWorkflowsGroup((Context) any, (Collection) any);
                    result = new AuthorizeException();
                    minTimes = 0;
            }
        };

        int step = 1;
        Group result = c.createWorkflowGroup(step);
        fail("Exception expected");
    }

    /**
     * Test of setWorkflowGroup method, of class Collection.
     */
    @Test
    public void testSetWorkflowGroup() throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        int step = 1;
        Group g = Group.create(context);
        context.restoreAuthSystemState();
        c.setWorkflowGroup(step, g);
        assertThat("testSetWorkflowGroup 0",c.getWorkflowGroup(step), notNullValue());
        assertThat("testSetWorkflowGroup 1",c.getWorkflowGroup(step), equalTo(g));
    }

    /**
     * Test of getWorkflowGroup method, of class Collection.
     */
    @Test
    public void testGetWorkflowGroup()
    {
        //null by default
        int step = 1;
        assertThat("testGetWorkflowGroup 0",c.getWorkflowGroup(step), nullValue());
    }

    /**
     * Test of createSubmitters method, of class Collection.
     */
    @Test
    public void testCreateSubmittersAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageSubmittersGroup((Context) any, (Collection) any);
                    result = null;
                    minTimes = 0;
            }
        };

        Group result = c.createSubmitters();
        assertThat("testCreateSubmittersAuth 0",result, notNullValue());
    }

    /**
     * Test of createSubmitters method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateSubmittersNoAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageSubmittersGroup((Context) any, (Collection) any);
                    result = new AuthorizeException();
                    minTimes = 0;
            }
        };

        Group result = c.createSubmitters();
        fail("Exception expected");
    }

    /**
     * Test of removeSubmitters method, of class Collection.
     */
    @Test
    public void testRemoveSubmittersAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageSubmittersGroup((Context) any, (Collection) any);
                    result = null;
                    minTimes = 0;
            }
        };

        c.removeSubmitters();
        assertThat("testRemoveSubmittersAuth 0", c.getSubmitters(), nullValue());
    }

    /**
     * Test of removeSubmitters method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveSubmittersNoAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageSubmittersGroup((Context) any, (Collection) any);
                    result = new AuthorizeException();
                    minTimes = 0;
            }
        };

        c.removeSubmitters();
        fail("Exception expected");
    }

    /**
     * Test of getSubmitters method, of class Collection.
     */
    @Test
    public void testGetSubmitters()
    {
        assertThat("testGetSubmitters 0", c.getSubmitters(), nullValue());
    }

    /**
     * Test of createAdministrators method, of class Collection.
     */
    @Test
    public void testCreateAdministratorsAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Collection) any);
                    result = null;
                    minTimes = 0;
            }
        };

        Group result = c.createAdministrators();
        assertThat("testCreateAdministratorsAuth 0", result, notNullValue());
    }

    /**
     * Test of createAdministrators method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateAdministratorsNoAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Collection) any);
                    result = new AuthorizeException();
                    minTimes = 0;
            }
        };

        Group result = c.createAdministrators();
        fail("Exception expected");
    }

    /**
     * Test of removeAdministrators method, of class Collection.
     */
    @Test
    public void testRemoveAdministratorsAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeRemoveAdminGroup((Context) any, (Collection) any);
                    result = null;
                    minTimes = 0;
            }
        };

        c.removeAdministrators();
        assertThat("testRemoveAdministratorsAuth 0", c.getAdministrators(), nullValue());
    }

    /**
     * Test of removeAdministrators method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveAdministratorsNoAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeRemoveAdminGroup((Context) any, (Collection) any);
                    result = new AuthorizeException();
                    minTimes = 0;
            }
        };

        c.removeAdministrators();
        fail("Exception expected");
    }

    /**
     * Test of getAdministrators method, of class Collection.
     */
    @Test
    public void testGetAdministrators()
    {
        assertThat("testGetAdministrators 0", c.getAdministrators(), nullValue());
    }

    /**
     * Test of getLicense method, of class Collection.
     */
    @Test
    public void testGetLicense() 
    {
        assertThat("testGetLicense 0", c.getLicense(), notNullValue());
        assertThat("testGetLicense 1", c.getLicense(), equalTo(ConfigurationManager.getDefaultSubmissionLicense()));
    }

    /**
     * Test of getLicenseCollection method, of class Collection.
     */
    @Test
    public void testGetLicenseCollection() 
    {
        assertThat("testGetLicenseCollection 0", c.getLicenseCollection(), notNullValue());
        assertThat("testGetLicenseCollection 1", c.getLicenseCollection(), equalTo(""));
    }

    /**
     * Test of hasCustomLicense method, of class Collection.
     */
    @Test
    public void testHasCustomLicense() 
    {
        assertFalse("testHasCustomLicense 0", c.hasCustomLicense());
    }

    /**
     * Test of setLicense method, of class Collection.
     */
    @Test
    public void testSetLicense() 
    {
        String license = "license for test";
        c.setLicense(license);
        assertThat("testSetLicense 0", c.getLicense(), notNullValue());
        assertThat("testSetLicense 1", c.getLicense(), equalTo(license));
        assertThat("testSetLicense 2", c.getLicenseCollection(), notNullValue());
        assertThat("testSetLicense 3", c.getLicenseCollection(), equalTo(license));
    }

    /**
     * Test of getTemplateItem method, of class Collection.
     */
    @Test
    public void testGetTemplateItem() throws Exception 
    {
        assertThat("testGetTemplateItem 0", c.getTemplateItem(), nullValue());
    }

    /**
     * Test of createTemplateItem method, of class Collection.
     */
    @Test
    public void testCreateTemplateItemAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = null;
                    minTimes = 0;
            }
        };

        c.createTemplateItem();
        assertThat("testCreateTemplateItemAuth 0",c.getTemplateItem(), notNullValue());
    }

    /**
     * Test of createTemplateItem method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateTemplateItemNoAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = new AuthorizeException();
                    minTimes = 0;
            }
        };

        c.createTemplateItem();
        fail("Exception expected");
    }

    /**
     * Test of removeTemplateItem method, of class Collection.
     */
    @Test
    public void testRemoveTemplateItemAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = null;
                    minTimes = 0;
            }
        };

        c.removeTemplateItem();
        assertThat("testRemoveTemplateItemAuth 0",c.getTemplateItem(), nullValue());
    }

    /**
     * Test of removeTemplateItem method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveTemplateItemNoAuth(@Mocked AuthorizeUtil authorizeUtil) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = new AuthorizeException();
                    minTimes = 0;
            }
        };

        c.removeTemplateItem();
        fail("Exception expected");
    }

    /**
     * Test of addItem method, of class Collection.
     */
    @Test
    public void testAddItemAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.ADD); result = null;
                        minTimes = 0;
            }
        };

        Item item = Item.create(context);
        c.addItem(item);
        boolean added = false;
        ItemIterator ii = c.getAllItems();
        while(ii.hasNext())
        {
            if(ii.next().equals(item))
            {
                added = true;
            }
        }
        assertTrue("testAddItemAuth 0",added);
    }

    /**
     * Test of addItem method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddItemNoAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.ADD); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        Item item = Item.create(context);
        c.addItem(item);
        fail("Excep[tion expected");
    }

    /**
     * Test of removeItem method, of class Collection.
     */
    @Test
    public void testRemoveItemAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.ADD); result = null;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.REMOVE); result = null;
                        minTimes = 0;
            }
        };

        Item item = Item.create(context);
        c.addItem(item);

        c.removeItem(item);
        boolean isthere = false;
        ItemIterator ii = c.getAllItems();
        while(ii.hasNext())
        {
            if(ii.next().equals(item))
            {
                isthere = true;
            }
        }
        assertFalse("testRemoveItemAuth 0",isthere);
    }

    /**
     * Test of removeItem method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveItemNoAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.ADD); result = null;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.REMOVE); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        Item item = Item.create(context);
        c.addItem(item);

        c.removeItem(item);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TODO: how to check update?
        c.update();
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth2(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TODO: how to check update?
        c.update();
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth3(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TODO: how to check update?
        c.update();
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth4(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TODO: how to check update?
        c.update();
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        c.update();
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth2 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth3(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth3 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth4(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth4 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        assertFalse("testCanEditBooleanNoAuth 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth_boolean 0", c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth2_boolean 0", c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth3_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth3_boolean 0", c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth4_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth4_boolean 0", c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth5_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth5_boolean 0", c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth6_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth6_boolean 0", c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth7_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth7_boolean 0", c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth8_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = null;
                        minTimes = 0;
            }
        };

        assertTrue("testCanEditBooleanAuth8_boolean 0", c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        assertFalse("testCanEditBooleanNoAuth_boolean 0",c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth2_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        assertFalse("testCanEditBooleanNoAuth_boolean 0",c.canEditBoolean(false));
    }
   
    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth_0args(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TODO: how to check??
        c.canEdit();
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth2_0args(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TODO: how to check??
        c.canEdit();
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth3_0args(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TODO: how to check??
        c.canEdit();
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth4_0args(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TODO: how to check??
        c.canEdit();
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth_0args(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        c.canEdit();
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth2_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth3_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth4_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = null;
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth5_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = null;
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth6_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = null;
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth7_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = true;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = null;
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth8_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = null;
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,false); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,false); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(false);
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth2_boolean(@Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD,true); result = false;
                        minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE,true); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        //TOO: how to check?
        c.canEdit(true);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class Collection.
     */
    @Test
    public void testDeleteAuth(@Mocked AuthorizeUtil authorizeUtil, @Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                 result = null;
                 minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE, anyBoolean); result = null;
                        minTimes = 0;
            }
        };

        int id = c.getID();
        c.delete();
        assertThat("testDelete 0",Collection.find(context, id),nullValue());
    }

    /**
     * Test of delete method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth(@Mocked AuthorizeUtil authorizeUtil, @Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                 result = new AuthorizeException();
                 minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE, anyBoolean); result = null;
                        minTimes = 0;
            }
        };

        int id = c.getID();
        c.delete();
        fail("Exception expected");
    }

     /**
     * Test of delete method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth2(@Mocked AuthorizeUtil authorizeUtil, @Mocked AuthorizeManager authorizeManager) throws Exception
    {
        new Expectations()
        {
            {
                AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                 result = null;
                 minTimes = 0;
                AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                        Constants.WRITE, anyBoolean); result = new AuthorizeException();
                        minTimes = 0;
            }
        };

        int id = c.getID();
        c.delete();
        fail("Exception expected");
    }

    /**
     * Test of getCommunities method, of class Collection.
     */
    @Test
    public void testGetCommunities() throws Exception
    {
        assertThat("testGetCommunities 0",c.getCommunities(), notNullValue());
        assertTrue("testGetCommunities 1",c.getCommunities().length == 0);
    }

    /**
     * Test of equals method, of class Collection.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals(@Mocked AuthorizeManager authorizeManager) throws SQLException, AuthorizeException
    {
        new Expectations()
        {
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Collection) any,
                        Constants.ADD); result = true;
                        minTimes = 0;
            }
        };

        assertFalse("testEquals 0",c.equals(null));
        assertFalse("testEquals 1",c.equals(Collection.create(context)));
        assertTrue("testEquals 2", c.equals(c));
    }

    /**
     * Test of getType method, of class Collection.
     */
    @Test
    @Override
    public void testGetType() 
    {
        assertThat("testGetType 0", c.getType(), equalTo(Constants.COLLECTION));
    }

    /**
     * Test of findAuthorized method, of class Collection.
     */
    @Test
    public void testFindAuthorized() throws Exception 
    {
        context.turnOffAuthorisationSystem();
        Community com = Community.create(null, context);
        context.restoreAuthSystemState();

        Collection[] found = Collection.findAuthorized(context, com, Constants.WRITE);
        assertThat("testFindAuthorized 0",found,notNullValue());
        assertTrue("testFindAuthorized 1",found.length == 0);

        found = Collection.findAuthorized(context, null, Constants.WRITE);
        assertThat("testFindAuthorized 2",found,notNullValue());
        assertTrue("testFindAuthorized 3",found.length == 0);

        found = Collection.findAuthorized(context, com, Constants.ADD);
        assertThat("testFindAuthorized 3",found,notNullValue());
        assertTrue("testFindAuthorized 4",found.length == 0);

        found = Collection.findAuthorized(context, null, Constants.ADD);
        assertThat("testFindAuthorized 5",found,notNullValue());
        assertTrue("testFindAuthorized 6",found.length == 0);

        found = Collection.findAuthorized(context, com, Constants.READ);
        assertThat("testFindAuthorized 7",found,notNullValue());
        assertTrue("testFindAuthorized 8",found.length == 0);

        found = Collection.findAuthorized(context, null, Constants.READ);
        assertThat("testFindAuthorized 9",found,notNullValue());
        assertTrue("testFindAuthorized 10",found.length >= 1);
    }

    /**
     * Test of countItems method, of class Collection.
     */
    @Test
    public void testCountItems() throws Exception
    {
        //0 by default
        assertTrue("testCountItems 0", c.countItems() == 0);
    }

    /**
     * Test of getAdminObject method, of class Collection.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException
    {
        //default community has no admin object
        assertThat("testGetAdminObject 0", (Collection)c.getAdminObject(Constants.REMOVE), equalTo(c));
        assertThat("testGetAdminObject 1", (Collection)c.getAdminObject(Constants.ADD), equalTo(c));
        assertThat("testGetAdminObject 2", c.getAdminObject(Constants.DELETE), nullValue());
        assertThat("testGetAdminObject 3", (Collection)c.getAdminObject(Constants.ADMIN), equalTo(c));
    }

    /**
     * Test of getParentObject method, of class Collection.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException
    {
        try
        {
            //default has no parent
            assertThat("testGetParentObject 0", c.getParentObject(), nullValue());

            context.turnOffAuthorisationSystem();
            Community parent = Community.create(null, context);
            parent.addCollection(c);
            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", c.getParentObject(), notNullValue());
            assertThat("testGetParentObject 2", (Community)c.getParentObject(), equalTo(parent));
        }
        catch(AuthorizeException ex)
        {
            fail("Authorize exception catched");
        }
    }

}
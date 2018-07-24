package com.testazure;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import com.microsoft.rest.LogLevel;
import rx.functions.Action1;
import java.io.File;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    static boolean complete = false;

    public static void main( String[] args )
    {
        try {    
            
            // Get the Azure settings for auth from file (azureauth.properties)
            
            final File credFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
            final int numberOfResources=GetNumberOfResourceGroupsToCreate(args);
            System.out.println(String.format("About to create and delete %d Resource Groups",numberOfResources));
            Scanner input = new Scanner(System.in);
            input.nextLine();

            // Generate a suffix name for each run 
            
            long suffix=System.nanoTime();

            final Region region=Region.UK_WEST;     

            // Log in to Azure

            final Azure azure = Azure.configure()
                .withLogLevel(LogLevel.BASIC)
                .withMaxIdleConnections(2)
                .authenticate(credFile)
                .withDefaultSubscription();
  
            // Create numberOfResources resource groups in parallel

            System.out.println(String.format("Starting Creating and deleting %d Resource Groups",numberOfResources));
            AtomicInteger leftToProcess = new AtomicInteger(numberOfResources);
            for (int i=0;i<numberOfResources;i++){
                
                final String rgName=String.format("javadeploymentTestRG-%d",(suffix+=i));
                azure.resourceGroups()
                    .define(rgName)
                    .withRegion(region)
                    .createAsync()
                    .subscribe(
                        new Action1<Indexable>(){
                            @Override 
                            public void call(Indexable createdRg){
                                System.out.println(String.format("Created resource group %s",rgName));
                                azure.resourceGroups()
                                    .deleteByNameAsync(rgName)
                                    .subscribe(
                                        () -> {
                                            System.out.println(String.format("Deleted resource group %s",rgName));
                                            System.out.println(String.format("% d Resource Groups Left To Process",leftToProcess.decrementAndGet()));
                                        },
                                        (Throwable t) -> {
                                            System.out.println(String.format("Failed To Delete resource group %s",rgName));
                                            System.out.println(String.format("% d Resource Groups Left To Process",leftToProcess.decrementAndGet()));
                                        }
                                    );                                    
                            }
                        }
                    );
            }

            // Wait for the RG create and delete to complete

            while (leftToProcess.get()>0){
                Thread.sleep(5000);
            }

            System.out.println(String.format("Finished Creating and deleting %d Resource Groups",numberOfResources));
            input.nextLine();
            input.close();
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
       
    }
    public static Integer GetNumberOfResourceGroupsToCreate(String[] args) 
    {
        Integer returnValue;
        try {
            returnValue = Integer.parseInt(args[0]);
        } catch (Exception e) {
            returnValue = 10; 
        }
        return returnValue;
      }
}
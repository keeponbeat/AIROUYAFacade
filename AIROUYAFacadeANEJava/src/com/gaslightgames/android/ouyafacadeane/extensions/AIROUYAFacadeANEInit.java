package com.gaslightgames.android.ouyafacadeane.extensions;

import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

import tv.ouya.console.api.OuyaFacade;
import android.content.IntentFilter;

import com.adobe.fre.FREByteArray;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;

public class AIROUYAFacadeANEInit implements FREFunction
{

	public FREObject call( FREContext context, FREObject[] passedArgs )
	{
		// Initialisation stuff...
		
		try
		{
			FREObject fro = passedArgs[0];
			String developerId = fro.getAsString();
			
			FREByteArray byteArray = (FREByteArray)passedArgs[1];
			byteArray.acquire();
			ByteBuffer byteBuffer = byteArray.getBytes();
			byteBuffer.order( null );	// Set to Little Endian.
			byteArray.release();
			
			// We cannot just call byteBuffer.array() as there are no guarantees we have an underlying
			// byte[] array.  Instead, we must loop through the Buffer and copy the data from the
			// target position in the loop.
			//byte[] bytes = byteBuffer.array();
			byte[] bytes = new byte[byteBuffer.limit()];
			byteBuffer.position(0);
			final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		    char[] hexChars = new char[bytes.length * 2];
		    int v;
		    
			int i = 0;
			while( byteBuffer.limit() > i )
			{
				bytes[i] = (byte)byteBuffer.get();
				v = bytes[i] & 0xFF;
				hexChars[ i * 2 ] = hexArray[ v >>> 4 ];
				hexChars[ i * 2 + 1 ] = hexArray[ v & 0x0F ];
				
				i++;
			}
			
			// Checking the Java side has decoded the Key correctly.
			//Log.i( "AIR_OUYA_IAP_INIT", "Key: " + new String(hexChars) );
			
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec( bytes );
			KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );
			((AIROUYAFacadeANEExtensionContext)context).publicKey = keyFactory.generatePublic( keySpec );
			
			((AIROUYAFacadeANEExtensionContext)context).ouyaFacade = OuyaFacade.getInstance();
			((AIROUYAFacadeANEExtensionContext)context).productListListener = new AIROUYAFacadeANEProductListListener( context );
			((AIROUYAFacadeANEExtensionContext)context).receiptListener = new AIROUYAFacadeANEReceiptListener( context );
			
			((AIROUYAFacadeANEExtensionContext)context).ouyaFacade.init( context.getActivity(), developerId );
		}
		catch( Exception exception )
		{
			exception.printStackTrace();
			
			((AIROUYAFacadeANEExtensionContext)context).dispatchStatusEventAsync( "INIT_FAILED", "Initialise Failed," + exception.getMessage() );
		}
		
		// Setup the Broadcast Receiver for the UPDATE event.
		((AIROUYAFacadeANEExtensionContext)context).broadcast = new AIROUYAFacadeANEBroadcastReceiver( (AIROUYAFacadeANEExtensionContext)context );
		String intentString = "tv.ouya.metrics.action.UPDATE";// "tv.ouya.intent.action.MENU_APPEARING";
		context.getActivity().registerReceiver( ((AIROUYAFacadeANEExtensionContext)context).broadcast, new IntentFilter(intentString) );
		
		return null;
	}

}

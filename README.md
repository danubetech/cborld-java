# cborld-java

## Information

This is a work-in-progress implementation of CBOR-LD.

Not ready for production use! Use at your own risk! Pull requests welcome.

## Maven

Build:

	mvn clean install

Dependency:

	<repositories>
		<repository>
			<id>danubetech-maven-public</id>
			<url>https://repo.danubetech.com/repository/maven-public/</url>
		</repository>
	</repositories>

	<dependency>
		<groupId>com.danubetech</groupId>
		<artifactId>cborld-java</artifactId>
		<version>1.0.0</version>
	</dependency>

## Example

Example code:

    // encode to CBOR-LD

    byte[] bytes = CborLdEncode.encode(jsonLdDocument, DOCUMENT_LOADER);
    System.out.println(Hex.encodeHex(bytes));
    
    // generate QR code
    
    byte[] qrCode = CborLdQrCode.toQrCode(bytes);
    File outputfile = new File("example-qrcode.png");
    OutputStream out = new FileOutputStream(outputfile);
    out.write(qrCode);
    out.close();
    
    // decode from CBOR-LD
    
    Map<String, Object> decoded = (Map<String, Object>) CborLdDecode.decode(bytes, DOCUMENT_LOADER);

Example QR code:

https://github.com/danubetech/cborld-java/blob/main/docs/example-qrcode.png

## About

Danube Tech - https://danubetech.com/

<br clear="left" />

<img align="left" height="70" src="https://raw.githubusercontent.com/danubetech/cborld-java/main/docs/logo-ngi-essiflab.png">

This software library is part of a project that has received funding from the European Union's Horizon 2020 research and innovation programme under grant agreement No 871932

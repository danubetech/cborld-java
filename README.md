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
		<version>0.1-SNAPSHOT</version>
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

<img align="left" src="https://raw.githubusercontent.com/danubetech/cborld-java/main/docs/example-qrcode.png?token=AACHA7NQWXYQ22RAQJBF2HLARFJPK">

## About

Danube Tech - https://danubetech.com/

const {desEncryptHex,desDecryptHex}=require("./crypto");
(async()=>{
  const combos=[["12345678","97b1ec16"]];
  const payload={installationID:0,brand:"VW",signStamp:0};
  for(const [REQK,RESK] of combos){
    const body={encryptedRequest:desEncryptHex(JSON.stringify(payload),REQK)};
    const r=await fetch("https://savwconnect.com/authentication/api/Authentication/1.0/authenticationEncryption",
      {method:"POST",headers:{"Content-Type":"application/json","Accept":"application/json","brand":"VW","User-Agent":"okhttp/4.12.0"},body:JSON.stringify(body)});
    const t=await r.text(); console.log("reqKey",REQK,"status",r.status);
    let j; try{j=JSON.parse(t);}catch{console.log("  raw:",t.slice(0,160));continue;}
    const enc=j.data||j.encryptedResponse||j.response;
    try{console.log("  =>",enc?desDecryptHex(enc,RESK):JSON.stringify(j));}
    catch(e){console.log("  decrypt-fail with",RESK,"raw:",JSON.stringify(j).slice(0,160));}
  }
})().catch(e=>console.error("ERR",e.message));

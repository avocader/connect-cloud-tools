## Finding deactivated pcc, that use a given pkc:
```
$ sudo -i psql -c "select distinct physical_cluster_id from deployment.logical_cluster where type = 'connect' and config->>'connector' like '%pkc-z35v3%' and deactivated is not null;"|grep pcc > deactivated_pccs.txt
``` 
Copy it from bastion to your local machine:

$ scp -i ~/.ssh/caas-alex.diachenko -i ~/.ssh/caas-alex.diachenko-stag-cert.pub alex.diachenko@0.bastion.us-west-2.aws.internal.stag.cpdev.cloud:/home/alex.diachenko/deactivated_pccs.txt ~/Desktop/deactivated_pccs.txt
...

## Finding Kafka endpoint and credentials by pkc:
```
mothership=> SELECT 
kc.config->'name' AS "satellite and region",
pc.config->'kafka'->'spec'->'spec'->'common'->'network'->'client_endpoints'->'external' AS "Kafka endpoint" 
FROM deployment.physical_cluster pc, deployment.k8s_cluster kc
WHERE pc.id = 'pkc-z35v3'
AND pc.k8s_cluster_id = kc.id;

    bastion and region     |                        Kafka endpoint
---------------------------+---------------------------------------------------------------
 "k8s-qeng-mz.us-central1" | "SASL_SSL://pkc-z35v3.us-central1.gcp.devel.cpdev.cloud:9092"
(1 row)
```

Find any active pcc, using Kafka endpoint from the previous step:
```
mothership=> SELECT physical_cluster_id 
FROM deployment.logical_cluster 
WHERE config->'connector'->'user_configs'->>'kafka.endpoint' = 'SASL_SSL://pkc-z35v3.us-central1.gcp.devel.cpdev.cloud:9092' 
AND deactivated IS NULL LIMIT 1;
 physical_cluster_id
---------------------
 pcc-6o3z8
(1 row)
```

Go to the satellite:
```
cclogin --cloud gcp --region us-central1 --env devel -b 0
```

Make sure you have all k8s contexts:
```
$ configure_kubecfg
```
Find k8s context:
```
$ kubectl config get-contexts |grep k8s-qeng-mz
*         gke_cc-devel_us-central1_k8s-qeng-mz-us-central1       gke_cc-devel_us-central1_c-jj2gc                   gke_cc-devel_us-central1_c-jj2gc
```
Switch to that k8s context:

```
$ kubectl config use-context gke_cc-devel_us-central1_k8s-qeng-mz-us-central1
```

Get credentials:
```
kubectl get secret -n pcc-6o3z8 apikeys -o yaml |grep connect-kafka-client.properties| cut -d' ' -f4 | base64 -d
username:<censored>
password:<censored>
```
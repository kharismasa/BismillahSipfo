import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

// ===== CONFIGURATION =====
const MIDTRANS_SERVER_KEY = "SB-Mid-server-SPe7qd8EkZ9DzRXCnEFuugwn";
const IS_PRODUCTION = false;
const MIDTRANS_API_URL = IS_PRODUCTION 
  ? "https://app.midtrans.com/snap/v1/transactions" 
  : "https://app.sandbox.midtrans.com/snap/v1/transactions";

// ===== SAFE SUPABASE CLIENT INITIALIZATION =====
let supabaseClient;
try {
  supabaseClient = createClient(
    Deno.env.get("SUPABASE_URL") || "https://ulxdrgkjbvalhxesibpr.supabase.co",
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVseGRyZ2tqYnZhbGh4ZXNpYnByIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mzk3MTgwOTUsImV4cCI6MjA1NTI5NDA5NX0.r7cDt4eJHFHELtNneP6_Q8SNl_Eg8Vj3GzVOIr9Pmr8"
  );
  console.log("✅ Supabase client initialized successfully");
} catch (error) {
  console.error("❌ Failed to initialize Supabase client:", error);
  supabaseClient = null;
}

// ===== SAFE HELPER FUNCTIONS =====
function safeFormatDateForPostgres(dateStr) {
  try {
    if (!dateStr) return null;
    
    // Already in YYYY-MM-DD format
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
      return dateStr;
    }
    
    // Convert DD/MM/YYYY to YYYY-MM-DD
    if (/^\d{2}\/\d{2}\/\d{4}$/.test(dateStr)) {
      const parts = dateStr.split('/');
      return `${parts[2]}-${parts[1]}-${parts[0]}`;
    }
    
    return dateStr;
  } catch (error) {
    console.error("Error formatting date:", error);
    return dateStr || null;
  }
}

function safeFormatPenggunaKhusus(value) {
  try {
    if (value === null || value === undefined) {
      return null;
    }
    
    const stringValue = String(value);
    
    // Handle enum values
    if (stringValue === 'INTERNAL_UII') {
      return 'Internal UII';
    } else if (stringValue === 'INTERNAL_VS_EKSTERNAL') {
      return 'Internal UII vs Team Eksternal';
    } else if (stringValue === 'EKSTERNAL_UII') {
      return 'Team Eksternal';
    }
    
    // Handle direct values
    if (['Internal UII', 'Internal UII vs Team Eksternal', 'Team Eksternal'].includes(stringValue)) {
      return stringValue;
    }
    
    return null;
  } catch (error) {
    console.error("Error formatting pengguna khusus:", error);
    return null;
  }
}

function safeMapMidtransStatus(transactionStatus, fraudStatus = "") {
  try {
    if (!transactionStatus) return "pending";
    
    const status = String(transactionStatus).toLowerCase();
    const fraud = String(fraudStatus).toLowerCase();
    
    if (status === "capture" || status === "settlement") {
      return fraud === "challenge" ? "pending" : "success";
    } else if (["cancel", "deny", "expire", "failure", "refund"].includes(status)) {
      return "failed";
    }
    
    return "pending";
  } catch (error) {
    console.error("Error mapping Midtrans status:", error);
    return "pending";
  }
}

// ===== SAFE DATABASE OPERATIONS =====
async function safeUpdatePeminjamanStatus(paymentId, status) {
  try {
    
    console.log(`Status update completed for payment ${paymentId}: ${status}`);
    console.log("Note: Peminjaman status is tracked via pembayaran.status_pembayaran");
    
    // Optional: Check if peminjaman records exist (for logging purposes)
    const { data: peminjaman, error: peminjamanError } = await supabaseClient
      .from('peminjaman_fasilitas')
      .select('id_peminjaman')
      .eq('id_pembayaran', paymentId);
    
    if (peminjamanError) {
      console.error("Error checking peminjaman:", peminjamanError);
      return false;
    }
    
    if (peminjaman && peminjaman.length > 0) {
      console.log(`Found ${peminjaman.length} peminjaman record(s) linked to payment ${paymentId}`);
      peminjaman.forEach(pem => {
        console.log(`- Peminjaman ID: ${pem.id_peminjaman} (status tracked via payment)`);
      });
    } else {
      console.log(`No peminjaman found for payment_id: ${paymentId}`);
    }
    
    return true;
  } catch (err) {
    console.error("Error in safeUpdatePeminjamanStatus:", err);
    return false;
  }
}

async function safeCreatePaymentRecord(paymentData) {
  try {
    if (!supabaseClient) {
      throw new Error("Database client not available");
    }

    const { data, error } = await Promise.race([
      supabaseClient
        .from('pembayaran')
        .insert(paymentData)
        .select()
        .single(),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error("Insert timeout")), 10000)
      )
    ]);

    if (error) {
      console.error("Error creating payment record:", error);
      throw error;
    }

    return data;
  } catch (error) {
    console.error("Error in safeCreatePaymentRecord:", error);
    throw error;
  }
}

async function safeCreatePeminjamanRecord(peminjamanData) {
  try {
    if (!supabaseClient) {
      throw new Error("Database client not available");
    }

    console.log("📝 Creating peminjaman with data:", JSON.stringify(peminjamanData, null, 2));

    const { data, error } = await Promise.race([
      supabaseClient
        .from('peminjaman_fasilitas')
        .insert(peminjamanData)
        .select(),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error("Peminjaman insert timeout")), 10000)
      )
    ]);

    if (error) {
      console.error("❌ Error creating peminjaman record:", error);
      throw error;
    }

    console.log("✅ Peminjaman record created successfully:", data);
    return data && data.length > 0 ? data[0] : null;
  } catch (error) {
    console.error("Error in safeCreatePeminjamanRecord:", error);
    throw error;
  }
}

async function safeInsertLapanganDipinjam(peminjamanId, lapanganIds) {
  try {
    if (!supabaseClient || !peminjamanId || !Array.isArray(lapanganIds)) {
      return false;
    }

    for (const lapanganId of lapanganIds) {
      try {
        await Promise.race([
          supabaseClient
            .from('lapangan_dipinjam')
            .insert({
              id_peminjaman: peminjamanId,
              id_lapangan: lapanganId
            }),
          new Promise((_, reject) => 
            setTimeout(() => reject(new Error("Lapangan insert timeout")), 5000)
          )
        ]);
        console.log(`✅ Inserted lapangan ${lapanganId} for peminjaman ${peminjamanId}`);
      } catch (lapanganError) {
        console.error(`❌ Error inserting lapangan ${lapanganId}:`, lapanganError);
      }
    }

    return true;
  } catch (error) {
    console.error("Error in safeInsertLapanganDipinjam:", error);
    return false;
  }
}

async function safeUpdateSuratUrl(peminjamanId, suratUrl) {
  try {
    if (!supabaseClient || !peminjamanId || !suratUrl) {
      console.warn("Missing parameters for surat URL update");
      return false;
    }

    console.log(`📄 Updating surat URL for peminjaman ${peminjamanId}: ${suratUrl}`);

    const { error } = await Promise.race([
      supabaseClient
        .from('peminjaman_fasilitas')
        .update({ surat_peminjaman_url: suratUrl })
        .eq('id_peminjaman', peminjamanId),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error("Surat URL update timeout")), 5000)
      )
    ]);

    if (error) {
      console.error("❌ Error updating surat URL:", error);
      return false;
    }

    console.log("✅ Surat URL updated successfully");
    return true;
  } catch (error) {
    console.error("Error in safeUpdateSuratUrl:", error);
    return false;
  }
}

// ===== MAIN REQUEST HANDLER =====
serve(async (req) => {
  // Set global timeout for the entire request
  const requestTimeout = setTimeout(() => {
    console.error("⏰ Request timeout - force terminating");
  }, 25000);

  try {
    const headers = {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Requested-With",
      "Cache-Control": "no-cache",
      "X-Function-Version": "2.0"
    };

    // Log request info
    const requestInfo = {
      method: req.method,
      url: req.url,
      timestamp: new Date().toISOString(),
      headers: Object.fromEntries(req.headers.entries())
    };
    console.log("📥 Request:", JSON.stringify(requestInfo, null, 2));

    // Handle preflight request
    if (req.method === "OPTIONS") {
      clearTimeout(requestTimeout);
      return new Response(null, { headers, status: 204 });
    }

    const url = new URL(req.url);
    const pathname = url.pathname;
    console.log(`🔍 Processing: ${req.method} ${pathname}`);

    // ===== HEALTH CHECK ENDPOINT =====
    if (pathname.endsWith('/health')) {
      clearTimeout(requestTimeout);
      
      try {
        let dbStatus = "disconnected";
        if (supabaseClient) {
          const { error } = await Promise.race([
            supabaseClient.from('pembayaran').select('count', { count: 'exact', head: true }),
            new Promise((_, reject) => setTimeout(() => reject(new Error("DB timeout")), 3000))
          ]);
          dbStatus = error ? "error" : "connected";
        }

        return new Response(JSON.stringify({
          success: true,
          message: "Function is healthy",
          timestamp: new Date().toISOString(),
          database: dbStatus,
          environment: {
            supabaseUrl: Deno.env.get("SUPABASE_URL") ? "configured" : "missing",
            serviceRoleKey: Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ? "configured" : "missing"
          }
        }), { headers, status: 200 });
      } catch (error) {
        return new Response(JSON.stringify({
          success: false,
          message: "Health check failed",
          error: error.message,
          timestamp: new Date().toISOString()
        }), { headers, status: 500 });
      }
    }

    // ===== WEBHOOK TEST ENDPOINT =====
    if (pathname.endsWith('/webhook-test')) {
      clearTimeout(requestTimeout);
      
      try {
        let testResult = { database: "not_tested" };
        
        if (supabaseClient) {
          const { error } = await Promise.race([
            supabaseClient.from('pembayaran').select('count', { count: 'exact', head: true }),
            new Promise((_, reject) => setTimeout(() => reject(new Error("DB timeout")), 5000))
          ]);
          
          testResult.database = error ? "connection_failed" : "connected";
          testResult.dbError = error?.message || null;
        }

        return new Response(JSON.stringify({
          success: true,
          message: "Webhook endpoint is working",
          timestamp: new Date().toISOString(),
          url: req.url,
          method: req.method,
          ...testResult
        }), { headers, status: 200 });
      } catch (error) {
        return new Response(JSON.stringify({
          success: false,
          message: "Webhook test failed",
          error: error.message,
          timestamp: new Date().toISOString()
        }), { headers, status: 500 });
      }
    }

    // ===== MIDTRANS NOTIFICATION WEBHOOK =====
    if (pathname.endsWith('/notification')) {
      console.log("🔔 Midtrans Notification Received");
      
      try {
        // Read request body with timeout
        const body = await Promise.race([
          req.text(),
          new Promise((_, reject) => 
            setTimeout(() => reject(new Error("Body read timeout")), 10000)
          )
        ]);

        console.log("📄 Raw body length:", body?.length || 0);
        console.log("📄 Raw body:", body);

        if (!body || body.trim() === '') {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Empty notification body"
          }), { headers, status: 400 });
        }

        // Parse JSON safely
        let notification;
        try {
          notification = JSON.parse(body);
        } catch (parseError) {
          console.error("❌ JSON parse error:", parseError);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Invalid JSON format",
            error: parseError.message
          }), { headers, status: 400 });
        }

        console.log("📊 Parsed notification:", JSON.stringify(notification, null, 2));

        // Extract required fields
        const paymentId = notification.order_id;
        const transactionStatus = notification.transaction_status;
        const fraudStatus = notification.fraud_status || "";
        const grossAmount = notification.gross_amount;
        const paymentType = notification.payment_type;

        console.log(`🔄 Processing: ID=${paymentId}, Status=${transactionStatus}, Fraud=${fraudStatus}, Amount=${grossAmount}, Type=${paymentType}`);

        // Validate required fields
        if (!paymentId) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Missing order_id in notification"
          }), { headers, status: 400 });
        }

        if (!transactionStatus) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Missing transaction_status in notification"
          }), { headers, status: 400 });
        }

        // Handle test notifications
        if (paymentId.includes("test") || paymentId.includes("TEST") || paymentId.includes("payment_notif_test")) {
          console.log("🧪 Test notification detected");
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: true,
            message: "Test notification processed successfully"
          }), { headers, status: 200 });
        }

        // Check database availability
        if (!supabaseClient) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Database service unavailable"
          }), { headers, status: 503 });
        }

        // Find existing payment
        const { data: existingPayment, error: checkError } = await Promise.race([
          supabaseClient
            .from('pembayaran')
            .select('id_pembayaran, status_pembayaran, total_biaya')
            .eq('id_pembayaran', paymentId)
            .single(),
          new Promise((_, reject) => 
            setTimeout(() => reject(new Error("Payment lookup timeout")), 8000)
          )
        ]);

        if (checkError || !existingPayment) {
          console.error("❌ Payment not found:", paymentId, checkError);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Payment not found in database",
            payment_id: paymentId,
            error: checkError?.message || "Not found"
          }), { headers, status: 404 });
        }

        console.log("💰 Found existing payment:", existingPayment);

        // Map Midtrans status to Supabase status
        const newStatus = safeMapMidtransStatus(transactionStatus, fraudStatus);
        console.log(`🔄 Status mapping: ${transactionStatus} -> ${newStatus}`);

        // Skip update if status unchanged
        if (existingPayment.status_pembayaran === newStatus) {
          console.log("⏭️ Status unchanged, skipping update");
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: true,
            message: `Status already ${newStatus}`,
            payment_id: paymentId,
            current_status: newStatus
          }), { headers, status: 200 });
        }

        // Update payment status
        const { error: updateError } = await Promise.race([
          supabaseClient
            .from('pembayaran')
            .update({ status_pembayaran: newStatus })
            .eq('id_pembayaran', paymentId),
          new Promise((_, reject) => 
            setTimeout(() => reject(new Error("Payment update timeout")), 8000)
          )
        ]);

        if (updateError) {
          console.error("❌ Payment update failed:", updateError);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Failed to update payment status",
            error: updateError.message
          }), { headers, status: 500 });
        }

        console.log(`✅ Payment status updated: ${paymentId} -> ${newStatus}`);

        // Update peminjaman status (fire-and-forget with error tolerance)
        safeUpdatePeminjamanStatus(paymentId, newStatus)
          .then(result => {
            if (!result) {
              console.warn("⚠️ Peminjaman status update failed, but payment was updated");
            }
          })
          .catch(error => {
            console.error("❌ Peminjaman update error:", error);
          });

        clearTimeout(requestTimeout);
        console.log("✅ Notification processing complete");

        return new Response(JSON.stringify({
          success: true,
          message: "Notification processed successfully",
          payment_id: paymentId,
          old_status: existingPayment.status_pembayaran,
          new_status: newStatus,
          processed_at: new Date().toISOString()
        }), { headers, status: 200 });

      } catch (error) {
        console.error("❌ Notification processing error:", error);
        clearTimeout(requestTimeout);
        return new Response(JSON.stringify({
          success: false,
          message: "Error processing notification",
          error: error.message,
          timestamp: new Date().toISOString()
        }), { headers, status: 500 });
      }
    }

    // ===== MAIN API ENDPOINTS (POST only) =====
    if (req.method !== "POST") {
      clearTimeout(requestTimeout);
      return new Response(JSON.stringify({
        error: "Method not allowed",
        allowed_methods: ["POST", "GET", "OPTIONS"],
        endpoint: pathname
      }), { status: 405, headers });
    }

    // ===== PARSE REQUEST BODY =====
    try {
      const body = await Promise.race([
        req.text(),
        new Promise((_, reject) => 
          setTimeout(() => reject(new Error("Request body timeout")), 15000)
        )
      ]);

      const data = JSON.parse(body);
      console.log("📨 Request data:", JSON.stringify(data, null, 2));

      // ===== ACTIVE STATUS POLLING ENDPOINT =====
      if (data.active_status_polling === true && data.payment_id) {
        const paymentId = data.payment_id;
        const pollCount = data.poll_count || 1;
        const maxPolls = 5;
        
        console.log(`🔄 Active status polling for ${paymentId} (attempt ${pollCount})`);

        if (!supabaseClient) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Database service unavailable"
          }), { status: 503, headers });
        }

        try {
          // STEP 1: Check current database status
          const { data: currentPayment, error: paymentError } = await Promise.race([
            supabaseClient
              .from('pembayaran')
              .select('id_pembayaran, status_pembayaran, created_at')
              .eq('id_pembayaran', paymentId)
              .single(),
            new Promise((_, reject) => 
              setTimeout(() => reject(new Error("Payment lookup timeout")), 5000)
            )
          ]);

          if (paymentError || !currentPayment) {
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: false,
              message: "Payment not found",
              payment_id: paymentId
            }), { headers, status: 404 });
          }

          console.log(`💰 Current payment status: ${currentPayment.status_pembayaran}`);

          // If already success or failed, return immediately
          if (currentPayment.status_pembayaran === 'success') {
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: true,
              status: 'success',
              message: "Payment already successful",
              final: true
            }), { headers, status: 200 });
          }

          if (currentPayment.status_pembayaran === 'failed') {
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: true,
              status: 'failed',
              message: "Payment failed",
              final: true
            }), { headers, status: 200 });
          }

          // STEP 2: For pending payments, check Midtrans status
          console.log(`🌐 Checking Midtrans status for pending payment: ${paymentId}`);
          
          const statusCheckUrl = IS_PRODUCTION 
            ? `https://api.midtrans.com/v2/${paymentId}/status`
            : `https://api.sandbox.midtrans.com/v2/${paymentId}/status`;

          const midtransRes = await Promise.race([
            fetch(statusCheckUrl, {
              method: "GET",
              headers: {
                Accept: "application/json",
                Authorization: "Basic " + btoa(MIDTRANS_SERVER_KEY + ":")
              }
            }),
            new Promise((_, reject) => 
              setTimeout(() => reject(new Error("Midtrans API timeout")), 10000)
            )
          ]);

          if (!midtransRes.ok) {
            console.warn(`⚠️ Midtrans API error: ${midtransRes.status}`);
            
            // Return current status if Midtrans error
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: true,
              status: currentPayment.status_pembayaran,
              message: "Midtrans API error, returning current status",
              final: false,
              should_retry: pollCount < maxPolls
            }), { headers, status: 200 });
          }

          const midtransResult = await midtransRes.json();
          console.log("📊 Midtrans polling response:", JSON.stringify(midtransResult, null, 2));

          // STEP 3: Enhanced status mapping
          const transactionStatus = midtransResult.transaction_status;
          const fraudStatus = midtransResult.fraud_status || "";
          const statusCode = midtransResult.status_code;

          console.log("🔍 Status details:");
          console.log(`  - Transaction Status: ${transactionStatus}`);
          console.log(`  - Fraud Status: ${fraudStatus}`);
          console.log(`  - Status Code: ${statusCode}`);

          let newStatus = "pending";
          let isFinal = false;

          // Enhanced mapping untuk polling
          if (transactionStatus === "settlement" || transactionStatus === "capture") {
            newStatus = fraudStatus === "challenge" ? "pending" : "success";
            isFinal = newStatus === "success";
            console.log("✅ Payment successful based on settlement/capture");
          } else if (transactionStatus === "pending") {
            // Untuk e-wallet dan VA, cek status_code juga
            if (statusCode === "200" || statusCode === 200) {
              newStatus = "success";
              isFinal = true;
              console.log("✅ Payment successful based on status_code 200");
            } else {
              newStatus = "pending";
              isFinal = false;
              console.log("⏳ Payment still pending");
            }
          } else if (["cancel", "deny", "expire", "failure", "refund"].includes(transactionStatus)) {
            newStatus = "failed";
            isFinal = true;
            console.log("❌ Payment failed");
          } else {
            newStatus = "pending";
            isFinal = false;
            console.log("❓ Unknown status, keeping as pending");
          }

          // STEP 4: Update database if status changed
          if (currentPayment.status_pembayaran !== newStatus) {
            console.log(`🔄 Updating payment status: ${currentPayment.status_pembayaran} → ${newStatus}`);
            
            const { error: updateError } = await Promise.race([
              supabaseClient
                .from('pembayaran')
                .update({ status_pembayaran: newStatus })
                .eq('id_pembayaran', paymentId),
              new Promise((_, reject) => 
                setTimeout(() => reject(new Error("Payment update timeout")), 8000)
              )
            ]);

            if (updateError) {
              console.error("❌ Payment update failed:", updateError);
            } else {
              console.log("✅ Payment status updated successfully");
            }
          }

          // STEP 5: Return polling result
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: true,
            status: newStatus,
            message: `Polling result: ${newStatus}`,
            final: isFinal,
            should_retry: !isFinal && pollCount < maxPolls,
            poll_count: pollCount,
            midtrans_data: {
              transaction_status: transactionStatus,
              fraud_status: fraudStatus,
              status_code: statusCode,
              payment_type: midtransResult.payment_type
            },
            previous_status: currentPayment.status_pembayaran
          }), { headers, status: 200 });

        } catch (error) {
          console.error("❌ Active polling error:", error);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Active polling failed",
            error: error.message,
            should_retry: pollCount < maxPolls
          }), { headers, status: 500 });
        }
      }

      // ===== FORCE UPDATE STATUS ENDPOINT (untuk debugging) =====
      if (data.force_update_status === true && data.payment_id) {
        const paymentId = data.payment_id;
        const forceStatus = data.force_status || "success";
        
        console.log(`🔧 Force updating status for ${paymentId} to ${forceStatus}`);

        if (!supabaseClient) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Database service unavailable"
          }), { status: 503, headers });
        }

        try {
          const { error } = await supabaseClient
            .from('pembayaran')
            .update({ status_pembayaran: forceStatus })
            .eq('id_pembayaran', paymentId);

          if (error) {
            console.error("❌ Force update failed:", error);
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: false,
              message: "Force update failed",
              error: error.message
            }), { headers, status: 500 });
          }

          console.log("✅ Force update successful");
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: true,
            message: `Status force updated to ${forceStatus}`,
            payment_id: paymentId,
            new_status: forceStatus
          }), { headers, status: 200 });

        } catch (error) {
          console.error("❌ Force update error:", error);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Force update error",
            error: error.message
          }), { headers, status: 500 });
        }
      }

      // ===== BATCH STATUS UPDATE ENDPOINT =====
      if (data.batch_status_update === true && data.payment_ids) {
        console.log("📦 Batch status update requested");
        
        const paymentIds = data.payment_ids;
        const results = [];
        
        if (!Array.isArray(paymentIds) || paymentIds.length === 0) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "payment_ids array required"
          }), { headers, status: 400 });
        }

        // Limit batch size untuk performance
        const batchPaymentIds = paymentIds.slice(0, 10);
        
        for (const paymentId of batchPaymentIds) {
          try {
            console.log(`🔄 Checking payment: ${paymentId}`);
            
            // Get current payment
            const { data: payment, error: paymentError } = await supabaseClient
              .from('pembayaran')
              .select('*')
              .eq('id_pembayaran', paymentId)
              .single();

            if (paymentError || !payment) {
              results.push({
                payment_id: paymentId,
                status: "not_found",
                error: paymentError?.message || "Payment not found"
              });
              continue;
            }

            // Skip if already final status
            if (payment.status_pembayaran === 'success' || payment.status_pembayaran === 'failed') {
              results.push({
                payment_id: paymentId,
                status: payment.status_pembayaran,
                action: "no_update_needed"
              });
              continue;
            }

            // Check Midtrans for pending payments
            try {
              const statusUrl = IS_PRODUCTION 
                ? `https://api.midtrans.com/v2/${paymentId}/status`
                : `https://api.sandbox.midtrans.com/v2/${paymentId}/status`;

              const midtransRes = await fetch(statusUrl, {
                headers: {
                  Accept: "application/json",
                  Authorization: "Basic " + btoa(MIDTRANS_SERVER_KEY + ":")
                }
              });

              if (midtransRes.ok) {
                const midtransData = await midtransRes.json();
                const newStatus = safeMapMidtransStatus(
                  midtransData.transaction_status, 
                  midtransData.fraud_status
                );

                if (payment.status_pembayaran !== newStatus) {
                  // Update status
                  const { error: updateError } = await supabaseClient
                    .from('pembayaran')
                    .update({ status_pembayaran: newStatus })
                    .eq('id_pembayaran', paymentId);

                  if (updateError) {
                    results.push({
                      payment_id: paymentId,
                      status: "update_failed",
                      error: updateError.message
                    });
                  } else {
                    results.push({
                      payment_id: paymentId,
                      status: newStatus,
                      action: "updated",
                      previous_status: payment.status_pembayaran
                    });
                  }
                } else {
                  results.push({
                    payment_id: paymentId,
                    status: newStatus,
                    action: "no_change"
                  });
                }
              } else {
                results.push({
                  payment_id: paymentId,
                  status: "midtrans_error",
                  error: `HTTP ${midtransRes.status}`
                });
              }
            } catch (midtransError) {
              results.push({
                payment_id: paymentId,
                status: "midtrans_error",
                error: midtransError.message
              });
            }
          } catch (error) {
            results.push({
              payment_id: paymentId,
              status: "error",
              error: error.message
            });
          }
        }

        clearTimeout(requestTimeout);
        return new Response(JSON.stringify({
          success: true,
          message: "Batch update completed",
          results: results,
          processed_count: results.length,
          timestamp: new Date().toISOString()
        }), { headers, status: 200 });
      }

      // ===== PAYMENT LIFECYCLE TRACKER =====
      if (data.track_payment_lifecycle === true && data.payment_id) {
        const paymentId = data.payment_id;
        
        console.log(`📈 Tracking payment lifecycle: ${paymentId}`);
        
        try {
          // Get payment history and current status
          const { data: payment, error: paymentError } = await supabaseClient
            .from('pembayaran')
            .select('*')
            .eq('id_pembayaran', paymentId)
            .single();

          if (paymentError || !payment) {
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: false,
              message: "Payment not found"
            }), { headers, status: 404 });
          }

          // Get related peminjaman
          const { data: peminjaman, error: peminjamanError } = await supabaseClient
            .from('peminjaman_fasilitas')
            .select('*')
            .eq('id_pembayaran', paymentId);

          // Calculate lifecycle info
          const createdAt = new Date(payment.created_at);
          const now = new Date();
          const ageMinutes = Math.floor((now - createdAt) / (1000 * 60));
          const expiryTime = new Date(payment.waktu_kadaluwarsa);
          const timeToExpiry = Math.floor((expiryTime - now) / (1000 * 60));

          // Check Midtrans status
          let midtransStatus = null;
          try {
            const statusUrl = IS_PRODUCTION 
              ? `https://api.midtrans.com/v2/${paymentId}/status`
              : `https://api.sandbox.midtrans.com/v2/${paymentId}/status`;

            const midtransRes = await fetch(statusUrl, {
              headers: {
                Accept: "application/json",
                Authorization: "Basic " + btoa(MIDTRANS_SERVER_KEY + ":")
              }
            });

            if (midtransRes.ok) {
              midtransStatus = await midtransRes.json();
            }
          } catch (midtransError) {
            console.warn("Midtrans status check failed:", midtransError.message);
          }

          const lifecycle = {
            payment_id: paymentId,
            database_status: payment.status_pembayaran,
            payment_method: payment.metode_pembayaran,
            amount: payment.total_biaya,
            created_at: payment.created_at,
            age_minutes: ageMinutes,
            expiry_time: payment.waktu_kadaluwarsa,
            time_to_expiry_minutes: timeToExpiry,
            is_expired: timeToExpiry <= 0,
            has_peminjaman: peminjaman && peminjaman.length > 0,
            midtrans_status: midtransStatus,
            status_sync: midtransStatus ? 
              (safeMapMidtransStatus(midtransStatus.transaction_status, midtransStatus.fraud_status) === payment.status_pembayaran) : 
              null,
            recommendations: []
          };

          // Add recommendations
          if (lifecycle.database_status === 'pending' && ageMinutes > 30) {
            lifecycle.recommendations.push("Consider checking Midtrans status - payment pending for > 30 minutes");
          }
          
          if (lifecycle.time_to_expiry_minutes <= 60 && lifecycle.time_to_expiry_minutes > 0) {
            lifecycle.recommendations.push("Payment will expire soon - consider extending or following up");
          }
          
          if (lifecycle.is_expired && lifecycle.database_status === 'pending') {
            lifecycle.recommendations.push("Payment is expired but still pending - should be marked as failed");
          }

          if (lifecycle.status_sync === false) {
            lifecycle.recommendations.push("Database status doesn't match Midtrans - needs sync");
          }

          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: true,
            lifecycle: lifecycle,
            timestamp: new Date().toISOString()
          }), { headers, status: 200 });

        } catch (error) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Lifecycle tracking failed",
            error: error.message
          }), { headers, status: 500 });
        }
      }

      // ===== DIRECT PROCESS REQUEST =====
      if (data.direct_process === true && data.payment_id) {
        console.log("⚡ Processing direct payment request:", data.payment_id);
        clearTimeout(requestTimeout);
        return new Response(JSON.stringify({
          success: true,
          payment_id: data.payment_id,
          message: "Direct processing successful"
        }), { status: 200, headers });
      }

      // ===== GENERATE MIDTRANS TOKEN =====
      if (data.generate_midtrans_token === true && data.payment_id) {
        console.log("🔑 Generating Midtrans token for:", data.payment_id);
        
        if (!supabaseClient) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Database service unavailable"
          }), { status: 503, headers });
        }

        // Check if payment exists
        const { data: payment, error: paymentError } = await Promise.race([
          supabaseClient
            .from('pembayaran')
            .select('*')
            .eq('id_pembayaran', data.payment_id)
            .single(),
          new Promise((_, reject) => 
            setTimeout(() => reject(new Error("Payment lookup timeout")), 5000)
          )
        ]);

        if (paymentError || !payment) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Payment not found",
            error: paymentError?.message
          }), { status: 404, headers });
        }

        // Prepare Midtrans request
        const midtransData = {
          transaction_details: {
            order_id: data.payment_id,
            gross_amount: data.transaction_details?.gross_amount || payment.total_biaya
          },
          customer_details: data.customer_details || {},
          item_details: data.item_details || []
        };

        console.log("🚀 Sending to Midtrans:", JSON.stringify(midtransData, null, 2));

        try {
          const midtransRes = await Promise.race([
            fetch(MIDTRANS_API_URL, {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
                Authorization: "Basic " + btoa(MIDTRANS_SERVER_KEY + ":")
              },
              body: JSON.stringify(midtransData)
            }),
            new Promise((_, reject) => 
              setTimeout(() => reject(new Error("Midtrans API timeout")), 15000)
            )
          ]);

          const midtransResult = await midtransRes.json();
          console.log("📥 Midtrans response:", JSON.stringify(midtransResult, null, 2));

          // Update payment with token
          if (midtransResult.token) {
            await supabaseClient
              .from('pembayaran')
              .update({
                midtrans_token: midtransResult.token,
                midtrans_redirect_url: midtransResult.redirect_url || null
              })
              .eq('id_pembayaran', data.payment_id);
          }

          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: true,
            payment_id: data.payment_id,
            ...midtransResult
          }), { status: midtransRes.status, headers });

        } catch (midtransError) {
          console.error("❌ Midtrans API error:", midtransError);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Midtrans API error",
            error: midtransError.message
          }), { status: 500, headers });
        }
      }

      // ===== UPDATE STATUS REQUEST =====
      if (data.update_status === true && data.payment_id) {
        const paymentId = data.payment_id;
        const newStatus = data.new_status || "success";
        
        console.log(`🔄 Updating payment status: ${paymentId} -> ${newStatus}`);

        if (!supabaseClient) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Database service unavailable"
          }), { status: 503, headers });
        }

        try {
          const { error } = await Promise.race([
            supabaseClient
              .from('pembayaran')
              .update({ status_pembayaran: newStatus })
              .eq('id_pembayaran', paymentId),
            new Promise((_, reject) => 
              setTimeout(() => reject(new Error("Update timeout")), 8000)
            )
          ]);

          if (error) {
            console.error("❌ Status update failed:", error);
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: false,
              message: "Status update failed",
              error: error.message
            }), { headers, status: 500 });
          }

          // Update peminjaman status
          await safeUpdatePeminjamanStatus(paymentId, newStatus);

          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: true,
            message: "Payment status updated successfully"
          }), { headers, status: 200 });

        } catch (error) {
          console.error("❌ Update status error:", error);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Update status failed",
            error: error.message
          }), { headers, status: 500 });
        }
      }

      // ===== CHECK TRANSACTION STATUS =====
      if (data.check_transaction_status === true && data.payment_id) {
        const paymentId = data.payment_id;
        console.log(`🔍 Checking transaction status for: ${paymentId}`);

        if (!supabaseClient) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Database service unavailable"
          }), { status: 503, headers });
        }

        try {
          // Get payment from database
          const { data: payment, error: paymentError } = await Promise.race([
            supabaseClient
              .from('pembayaran')
              .select('*')
              .eq('id_pembayaran', paymentId)
              .single(),
            new Promise((_, reject) => 
              setTimeout(() => reject(new Error("Payment lookup timeout")), 5000)
            )
          ]);

          if (paymentError || !payment) {
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: false,
              message: "Payment not found"
            }), { headers, status: 404 });
          }

          // If already success, return immediately
          if (payment.status_pembayaran === 'success') {
            await safeUpdatePeminjamanStatus(paymentId, 'success');
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: true,
              status: 'success',
              message: "Payment already successful"
            }), { headers, status: 200 });
          }

          // Check status from Midtrans
          const statusCheckUrl = IS_PRODUCTION 
            ? `https://api.midtrans.com/v2/${paymentId}/status`
            : `https://api.sandbox.midtrans.com/v2/${paymentId}/status`;

          const midtransRes = await Promise.race([
            fetch(statusCheckUrl, {
              method: "GET",
              headers: {
                Accept: "application/json",
                Authorization: "Basic " + btoa(MIDTRANS_SERVER_KEY + ":")
              }
            }),
            new Promise((_, reject) => 
              setTimeout(() => reject(new Error("Midtrans status check timeout")), 10000)
            )
          ]);

          const midtransResult = await midtransRes.json();
          console.log("📊 Midtrans status response:", JSON.stringify(midtransResult, null, 2));

          // Map status
          const newStatus = safeMapMidtransStatus(
            midtransResult.transaction_status, 
            midtransResult.fraud_status
          );

          // Update payment status
          await supabaseClient
            .from('pembayaran')
            .update({ status_pembayaran: newStatus })
            .eq('id_pembayaran', paymentId);

          // Update peminjaman status
          await safeUpdatePeminjamanStatus(paymentId, newStatus);

          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: true,
            status: newStatus,
            message: `Payment status updated to ${newStatus}`
          }), { headers, status: 200 });

        } catch (error) {
          console.error("❌ Transaction status check error:", error);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Transaction status check failed",
            error: error.message
          }), { headers, status: 500 });
        }
      }

      // ===== UPDATE SURAT URL ENDPOINT =====
      if (data.update_surat_url === true && data.id_peminjaman && data.surat_url) {
        console.log(`📄 Updating surat URL for peminjaman ID: ${data.id_peminjaman}`);
        
        if (!supabaseClient) {
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Database service unavailable"
          }), { status: 503, headers });
        }

        try {
          const updateResult = await safeUpdateSuratUrl(data.id_peminjaman, data.surat_url);
          
          if (updateResult) {
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: true,
              message: "Surat URL updated successfully"
            }), { headers, status: 200 });
          } else {
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: false,
              message: "Failed to update surat URL"
            }), { headers, status: 500 });
          }
        } catch (err) {
          console.error("❌ Error in surat URL update process:", err);
          clearTimeout(requestTimeout);
          return new Response(JSON.stringify({
            success: false,
            message: "Error updating surat URL",
            error: err.message
          }), { headers, status: 500 });
        }
      }

      // ===== CREATE NEW PAYMENT (DEFAULT CASE) =====
      console.log("💳 Creating new payment...");
      
      if (!supabaseClient) {
        clearTimeout(requestTimeout);
        return new Response(JSON.stringify({
          success: false,
          message: "Database service unavailable"
        }), { status: 503, headers });
      }

      // Generate unique payment ID
      const paymentId = `PAY-${Date.now()}-${Math.floor(Math.random() * 1000)}`;

      // Extract basic information
      const totalAmount = data.transaction_details?.gross_amount || 0;
      const idPengguna = data.id_pengguna;
      const idFasilitas = data.id_fasilitas;
      const idVoucher = data.id_voucher || null;
      const saveDataOnly = data.save_data_only === true;

      // Check if free booking
      const isFreeBooking = data.is_free_booking === true || 
                           totalAmount === 0 || 
                           (data.peminjaman_data && data.peminjaman_data.opsi_peminjaman === "Sesuai Jadwal Rutin");

      console.log(`💰 Payment details: ID=${paymentId}, Amount=${totalAmount}, Free=${isFreeBooking}, User=${idPengguna}, Facility=${idFasilitas}`);

      // Set expiry time
      const expiryTime = new Date();
      expiryTime.setHours(expiryTime.getHours() + 24);

      // Create payment record
      const paymentData = {
        id_pembayaran: paymentId,
        id_voucher: idVoucher,
        metode_pembayaran: isFreeBooking ? 'Gratis' : 'Midtrans',
        status_pembayaran: isFreeBooking ? 'success' : 'pending',
        total_biaya: totalAmount,
        created_at: new Date().toISOString(),
        waktu_kadaluwarsa: expiryTime.toISOString(),
        midtrans_token: null,
        midtrans_redirect_url: null
      };

      try {
        const payment = await safeCreatePaymentRecord(paymentData);
        console.log("✅ Payment record created:", payment.id_pembayaran);
      } catch (paymentError) {
        console.error("❌ Payment creation failed:", paymentError);
        clearTimeout(requestTimeout);
        return new Response(JSON.stringify({
          success: false,
          message: "Payment creation failed",
          error: paymentError.message
        }), { status: 500, headers });
      }

      // Create peminjaman if data available
      let peminjamanId = null;
      if (data.peminjaman_data) {
        try {
          const peminjamanData = data.peminjaman_data;
          
          const peminjamanInsertData = {
            id_fasilitas: idFasilitas,
            tanggal_mulai: safeFormatDateForPostgres(peminjamanData.tanggal_mulai),
            tanggal_selesai: safeFormatDateForPostgres(peminjamanData.tanggal_selesai),
            jam_mulai: peminjamanData.jam_mulai,
            jam_selesai: peminjamanData.jam_selesai,
            nama_organisasi: peminjamanData.nama_organisasi || "",
            nama_acara: peminjamanData.nama_acara || "",
            id_pembayaran: paymentId,
            pengguna_khusus: safeFormatPenggunaKhusus(peminjamanData.pengguna_khusus),
            id_pengguna: idPengguna,
            created_at: new Date().toISOString(),
            surat_peminjaman_url: peminjamanData.surat_peminjaman_url || null
          };

          console.log("📝 Creating peminjaman:", JSON.stringify(peminjamanInsertData, null, 2));

          const peminjaman = await safeCreatePeminjamanRecord(peminjamanInsertData);
          if (peminjaman) {
            peminjamanId = peminjaman.id_peminjaman;
            console.log("✅ Peminjaman created:", peminjamanId);

            // Log surat URL if provided
            if (peminjamanInsertData.surat_peminjaman_url) {
              console.log("📄 Surat URL saved:", peminjamanInsertData.surat_peminjaman_url);
            }

            // Process lapangan_ids
            if (peminjamanData.lapangan_ids && Array.isArray(peminjamanData.lapangan_ids)) {
              console.log("🏟️ Processing lapangan IDs:", peminjamanData.lapangan_ids);
              await safeInsertLapanganDipinjam(peminjamanId, peminjamanData.lapangan_ids);
            }
          }
        } catch (peminjamanError) {
          console.error("❌ Peminjaman creation failed:", peminjamanError);
          // Don't fail the entire request for peminjaman errors in case of paid bookings
          if (isFreeBooking) {
            clearTimeout(requestTimeout);
            return new Response(JSON.stringify({
              success: false,
              message: "Peminjaman creation failed for free booking",
              error: peminjamanError.message
            }), { status: 500, headers });
          }
        }
      }

      // For free bookings, return success
      if (isFreeBooking) {
        clearTimeout(requestTimeout);
        console.log("✅ Free booking completed successfully");
        return new Response(JSON.stringify({
          success: true,
          payment_id: paymentId,
          status: "success", 
          message: "Peminjaman gratis berhasil dibuat",
          is_free_booking: true, 
          peminjaman_id: peminjamanId
        }), { status: 200, headers });
      }

      // For save_data_only, return without Midtrans
      if (saveDataOnly) {
        clearTimeout(requestTimeout);
        return new Response(JSON.stringify({
          success: true,
          payment_id: paymentId,
          message: "Data saved successfully"
        }), { status: 200, headers });
      }

      // Generate Midtrans token
      try {
        const midtransData = {
          transaction_details: {
            order_id: paymentId,
            gross_amount: totalAmount
          },
          customer_details: data.customer_details || {},
          item_details: data.item_details || []
        };

        console.log("🚀 Sending to Midtrans:", JSON.stringify(midtransData, null, 2));

        const midtransRes = await Promise.race([
          fetch(MIDTRANS_API_URL, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
              Authorization: "Basic " + btoa(MIDTRANS_SERVER_KEY + ":")
            },
            body: JSON.stringify(midtransData)
          }),
          new Promise((_, reject) => 
            setTimeout(() => reject(new Error("Midtrans API timeout")), 15000)
          )
        ]);

        const midtransResult = await midtransRes.json();
        console.log("📥 Midtrans response:", JSON.stringify(midtransResult, null, 2));

        // Update payment with token
        if (midtransResult.token) {
          await supabaseClient
            .from('pembayaran')
            .update({
              midtrans_token: midtransResult.token,
              midtrans_redirect_url: midtransResult.redirect_url || null
            })
            .eq('id_pembayaran', paymentId);
        }

        clearTimeout(requestTimeout);
        console.log("✅ Midtrans payment flow completed");
        return new Response(JSON.stringify({
          success: true,
          payment_id: paymentId,
          token: midtransResult.token, 
          redirect_url: midtransResult.redirect_url, 
          status: "pending", 
          message: "Payment token generated successfully",
          peminjaman_id: peminjamanId
        }), { status: 200, headers });

      } catch (midtransError) {
        console.error("❌ Midtrans error:", midtransError);
        clearTimeout(requestTimeout);
        return new Response(JSON.stringify({
          success: false,
          message: "Midtrans API error",
          payment_id: paymentId,
          error: midtransError.message
        }), { status: 500, headers });
      }

    } catch (error) {
      console.error("❌ Request processing error:", error);
      clearTimeout(requestTimeout);
      return new Response(JSON.stringify({
        success: false,
        message: "Request processing failed",
        error: error.message
      }), { status: 500, headers });
    }

  } catch (error) {
    console.error("❌ Fatal request error:", error);
    clearTimeout(requestTimeout);
    return new Response(JSON.stringify({
      success: false,
      message: "Fatal error occurred",
      error: error.message,
      timestamp: new Date().toISOString()
    }), {
      status: 500,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*"
      }
    });
  }
});